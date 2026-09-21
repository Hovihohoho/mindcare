"""Train and validate MindCare's PMData stress-signal model offline."""
from __future__ import annotations

import csv, hashlib, json
from collections import defaultdict
from datetime import timedelta
from pathlib import Path
from typing import Iterator

import ijson, joblib, numpy as np, onnxruntime as ort, pandas as pd
from sklearn.base import clone
from sklearn.ensemble import ExtraTreesClassifier, RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, balanced_accuracy_score, classification_report, confusion_matrix, f1_score, mean_absolute_error
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from skl2onnx import to_onnx

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_ROOT = PROJECT_ROOT / "training" / "osfstorage-archive" / "pmdata"
MODEL_DIR = PROJECT_ROOT / "ml-training" / "models"
REPORT_DIR = PROJECT_ROOT / "ml-training" / "reports"
MODEL_VERSION = "stress-classifier-v3"
STRESS_SCORES = [1, 2, 3, 4, 5]
STRESS_SCORE_NAMES = [f"STRESS_SCORE_{score}" for score in STRESS_SCORES]
BASE_FEATURES = ["hr_mean", "hr_std", "hr_count", "steps_total", "sleep_minutes", "sleep_efficiency", "sleep_deep_minutes", "sleep_rem_minutes", "sleep_awake_minutes", "resting_hr"]
TREND_SOURCES = ["hr_mean", "steps_total", "sleep_minutes", "sleep_efficiency", "resting_hr"]
FEATURES = BASE_FEATURES + [f"{name}_{suffix}" for name in TREND_SOURCES for suffix in ("mean_3d", "mean_7d", "delta_7d")]


def stream_json_array(path: Path) -> Iterator[dict]:
    if path.exists():
        with path.open("rb") as handle:
            yield from ijson.items(handle, "item")


def utc_day(value: object) -> str:
    return pd.Timestamp(value).date().isoformat()


def safe_float(value: object) -> float | None:
    try:
        parsed = float(value)
        return parsed if np.isfinite(parsed) else None
    except (TypeError, ValueError):
        return None


def daily_features(fitbit: Path) -> pd.DataFrame:
    result: dict[str, dict[str, float]] = defaultdict(dict)
    hr_stats: dict[str, list[float]] = defaultdict(lambda: [0.0, 0.0, 0.0])
    for item in stream_json_array(fitbit / "heart_rate.json"):
        raw_value = item.get("value")
        value = safe_float(raw_value.get("bpm") if isinstance(raw_value, dict) else raw_value)
        if value is None or not 20 <= value <= 250:
            continue
        stats = hr_stats[utc_day(item.get("dateTime"))]
        stats[0] += value; stats[1] += value * value; stats[2] += 1
    for feature_day, (total, squares, count) in hr_stats.items():
        mean = total / count
        result[feature_day].update(hr_mean=mean, hr_std=max(0.0, squares / count - mean * mean) ** 0.5, hr_count=count)

    steps: dict[str, float] = defaultdict(float)
    for item in stream_json_array(fitbit / "steps.json"):
        raw_value = item.get("value")
        value = safe_float(raw_value.get("value") if isinstance(raw_value, dict) else raw_value)
        if value is not None and 0 <= value <= 10_000:
            steps[utc_day(item.get("dateTime"))] += value
    for feature_day, total in steps.items(): result[feature_day]["steps_total"] = total

    for item in stream_json_array(fitbit / "sleep.json"):
        if not item.get("mainSleep", True) or not item.get("dateOfSleep"): continue
        summary = item.get("levels", {}).get("summary", {})
        result[str(item["dateOfSleep"])].update(
            sleep_minutes=safe_float(item.get("minutesAsleep")), sleep_efficiency=safe_float(item.get("efficiency")),
            sleep_deep_minutes=safe_float(summary.get("deep", {}).get("minutes")), sleep_rem_minutes=safe_float(summary.get("rem", {}).get("minutes")),
            sleep_awake_minutes=safe_float(item.get("minutesAwake")))

    for item in stream_json_array(fitbit / "resting_heart_rate.json"):
        raw_value = item.get("value")
        value = safe_float(raw_value.get("value") if isinstance(raw_value, dict) else raw_value)
        if value is not None and 20 <= value <= 150: result[utc_day(item.get("dateTime"))]["resting_hr"] = value

    frame = pd.DataFrame.from_dict(result, orient="index").rename_axis("feature_day").sort_index()
    if frame.empty: return frame
    frame.index = pd.to_datetime(frame.index)
    frame = frame.reindex(pd.date_range(frame.index.min(), frame.index.max(), freq="D"))
    frame = frame.reindex(columns=BASE_FEATURES)
    for name in TREND_SOURCES:
        history = frame[name].shift(1)
        frame[f"{name}_mean_3d"] = history.rolling(3, min_periods=2).mean()
        frame[f"{name}_mean_7d"] = history.rolling(7, min_periods=3).mean()
        frame[f"{name}_delta_7d"] = frame[name] - frame[f"{name}_mean_7d"]
    frame.index = frame.index.strftime("%Y-%m-%d")
    return frame


def build_dataset() -> tuple[pd.DataFrame, int]:
    rows: list[dict[str, object]] = []
    excluded_out_of_scale = 0
    for participant in sorted(DATA_ROOT.glob("p[0-9][0-9]")):
        print(f"Reading {participant.name}...", flush=True)
        daily = daily_features(participant / "fitbit")
        with (participant / "pmsys" / "wellness.csv").open(encoding="utf-8-sig", newline="") as handle:
            for wellness in csv.DictReader(handle):
                stress = safe_float(wellness.get("stress"))
                if stress is None: continue
                if not stress.is_integer() or int(stress) not in STRESS_SCORES:
                    excluded_out_of_scale += 1
                    continue
                feature_day = (pd.Timestamp(wellness["effective_time_frame"]).date() - timedelta(days=1)).isoformat()
                if feature_day not in daily.index: continue
                values = daily.loc[feature_day]
                row = {name: values.get(name, np.nan) for name in FEATURES}
                if all(pd.isna(row[name]) for name in BASE_FEATURES): continue
                row.update(participant=participant.name, feature_day=feature_day, target=int(stress))
                rows.append(row)
    frame = pd.DataFrame(rows)
    if frame.empty or sorted(frame["target"].unique().tolist()) != STRESS_SCORES:
        raise RuntimeError("PMData did not produce all five documented stress scores")
    return frame, excluded_out_of_scale


def candidates() -> dict[str, Pipeline]:
    return {
        "logistic_regression": Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler()), ("classifier", LogisticRegression(C=.3, class_weight="balanced", max_iter=2000, random_state=42))]),
        "random_forest": Pipeline([("imputer", SimpleImputer(strategy="median")), ("classifier", RandomForestClassifier(n_estimators=500, max_depth=7, min_samples_leaf=4, max_features="sqrt", class_weight="balanced_subsample", random_state=42, n_jobs=-1))]),
        "extra_trees": Pipeline([("imputer", SimpleImputer(strategy="median")), ("classifier", ExtraTreesClassifier(n_estimators=500, max_depth=8, min_samples_leaf=3, max_features="sqrt", class_weight="balanced", random_state=42, n_jobs=-1))]),
    }


def evaluate(name: str, estimator: Pipeline, x: pd.DataFrame, y: pd.Series, groups: pd.Series) -> dict:
    predictions = np.zeros(len(y), dtype=int)
    for train, test in LeaveOneGroupOut().split(x, y, groups):
        predictions[test] = clone(estimator).fit(x.iloc[train], y.iloc[train]).predict(x.iloc[test])
    report = classification_report(y, predictions, labels=STRESS_SCORES, target_names=STRESS_SCORE_NAMES, output_dict=True, zero_division=0)
    return {
        "model": name,
        "accuracy": round(float(accuracy_score(y, predictions)), 4),
        "balancedAccuracy": round(float(balanced_accuracy_score(y, predictions)), 4),
        "f1Macro": round(float(f1_score(y, predictions, labels=STRESS_SCORES, average="macro", zero_division=0)), 4),
        "meanAbsoluteError": round(float(mean_absolute_error(y, predictions)), 4),
        "withinOnePointAccuracy": round(float(np.mean(np.abs(y.to_numpy() - predictions) <= 1)), 4),
        "perScore": {name: report[name] for name in STRESS_SCORE_NAMES},
        "confusionMatrix": confusion_matrix(y, predictions, labels=STRESS_SCORES).tolist(),
    }


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""): digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    if not DATA_ROOT.exists(): raise FileNotFoundError(f"PMData folder not found: {DATA_ROOT}")
    MODEL_DIR.mkdir(parents=True, exist_ok=True); REPORT_DIR.mkdir(parents=True, exist_ok=True)
    data, excluded_out_of_scale = build_dataset(); x = data[FEATURES].astype(np.float32); y = data["target"].astype(int); groups = data["participant"].astype(str)
    evaluations = []
    for name, candidate in candidates().items():
        print(f"Evaluating {name}...", flush=True); evaluations.append(evaluate(name, candidate, x, y, groups))
    winner = max(evaluations, key=lambda item: (item["f1Macro"], -item["meanAbsoluteError"]))
    model = candidates()[winner["model"]].fit(x, y)
    joblib_path = MODEL_DIR / f"{MODEL_VERSION}.joblib"; onnx_path = MODEL_DIR / f"{MODEL_VERSION}.onnx"
    joblib.dump(model, joblib_path)
    converted = to_onnx(model, x.iloc[:1].to_numpy(dtype=np.float32), target_opset=17, options={id(model.named_steps["classifier"]): {"zipmap": False}})
    onnx_path.write_bytes(converted.SerializeToString())
    sample = x.iloc[:16].to_numpy(dtype=np.float32); session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    onnx_labels = np.asarray(session.run(None, {session.get_inputs()[0].name: sample})[0]).reshape(-1).astype(int)
    if not np.array_equal(onnx_labels, model.predict(x.iloc[:16]).astype(int)): raise RuntimeError("ONNX predictions do not match Python")
    metrics = {"evaluation": "LeaveOneParticipantOut", "samples": int(len(data)), "participants": int(groups.nunique()), "classDistribution": {str(score): int((y == score).sum()) for score in STRESS_SCORES}, "excludedOutOfScaleLabels": excluded_out_of_scale, "selectedModel": winner["model"], "models": evaluations, "onnxParitySamples": 16, "onnxParityPassed": True}
    metadata = {"modelVersion": MODEL_VERSION, "dataset": "PMData", "selectedModel": winner["model"], "features": FEATURES, "labels": {"1": "BELOW_NORMAL", "2": "BELOW_NORMAL", "3": "NORMAL", "4": "ABOVE_NORMAL", "5": "ABOVE_NORMAL"}, "scale": {"minimum": 1, "maximum": 5, "normalScore": 3, "source": "PMData/PMSys wellness self-report scale"}, "target": "next-day self-reported PMData stress score (1-5)", "featureWindow": "prior day plus trailing 3/7-day history", "excludedLabels": "Values outside the paper-defined 1-5 scale are excluded", "onnxSha256": sha256(onnx_path), "usageNotice": "Estimate of the PMData self-reported wellness score; not a clinical diagnosis."}
    serialized_metrics = json.dumps(metrics, indent=2)
    (REPORT_DIR / "metrics-v3.json").write_text(serialized_metrics, encoding="utf-8")
    (REPORT_DIR / "metrics.json").write_text(serialized_metrics, encoding="utf-8")
    (MODEL_DIR / f"{MODEL_VERSION}.metadata.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__": main()

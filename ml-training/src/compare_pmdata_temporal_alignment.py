"""Train and compare two PMData temporal alignments.

Model A (current): stress survey on day D is predicted from every metric stored
under D-1, including Fitbit sleep whose ``dateOfSleep`` is D-1.

Model B (morning alignment): stress survey on day D is predicted from activity
and heart-rate metrics on D-1 plus the main sleep ending on D, provided that
the sleep ended no later than the survey timestamp.

The script deliberately writes Model B to a separate v4 artifact. It does not
replace the v3 artifact used by the running services.
"""

from __future__ import annotations

import hashlib
import json
from collections import Counter
from datetime import timedelta
from pathlib import Path

import ijson
import joblib
import numpy as np
import onnxruntime as ort
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    mean_absolute_error,
    precision_score,
    recall_score,
)
from sklearn.base import clone
from skl2onnx import to_onnx

import train_pmdata as pmdata


MODEL_B_VERSION = "stress-classifier-v4"
MODEL_B_FEATURE_VERSION = "pmdata-features-v3-morning"
SLEEP_FEATURES = [
    "sleep_minutes",
    "sleep_efficiency",
    "sleep_deep_minutes",
    "sleep_rem_minutes",
    "sleep_awake_minutes",
]


def parse_timestamp(value: str) -> pd.Timestamp:
    """Return a timezone-naive timestamp suitable for PMData comparisons."""
    timestamp = pd.Timestamp(value)
    if timestamp.tzinfo is not None:
        timestamp = timestamp.tz_convert("UTC").tz_localize(None)
    return timestamp


def load_sleep_end_times(path: Path) -> dict[pd.Timestamp, pd.Timestamp]:
    """Match the same last-main-sleep-per-day behavior used by train_pmdata."""
    result: dict[pd.Timestamp, pd.Timestamp] = {}
    if not path.exists():
        return result

    with path.open("rb") as handle:
        for item in ijson.items(handle, "item"):
            if not item.get("mainSleep", True):
                continue
            date_of_sleep = item.get("dateOfSleep")
            end_time = item.get("endTime")
            if not date_of_sleep or not end_time:
                continue
            try:
                result[pd.Timestamp(date_of_sleep).normalize()] = parse_timestamp(end_time)
            except (TypeError, ValueError):
                continue
    return result


def add_trends(frame: pd.DataFrame) -> pd.DataFrame:
    result = frame.copy()
    for name in pmdata.TREND_SOURCES:
        history = result[name].shift(1)
        result[f"{name}_mean_3d"] = history.rolling(3, min_periods=2).mean()
        result[f"{name}_mean_7d"] = history.rolling(7, min_periods=3).mean()
        result[f"{name}_delta_7d"] = result[name] - result[f"{name}_mean_7d"]
    return result


def build_model_b_frame(model_a_frame: pd.DataFrame) -> pd.DataFrame:
    """Attach sleep ending next morning to the preceding activity day."""
    result = model_a_frame[pmdata.BASE_FEATURES].copy()
    for feature in SLEEP_FEATURES:
        result[feature] = model_a_frame[feature].shift(-1)
    return add_trends(result)


def mask_sleep_after_survey(row: pd.Series) -> pd.Series:
    masked = row.copy()
    for feature in SLEEP_FEATURES:
        masked[feature] = np.nan
    masked["sleep_minutes_delta_7d"] = np.nan
    return masked


def build_comparison_datasets() -> tuple[pd.DataFrame, pd.DataFrame, dict]:
    rows_a: list[dict] = []
    rows_b: list[dict] = []
    diagnostics = Counter()

    participant_dirs = sorted(
        path
        for path in pmdata.DATA_ROOT.glob("p[0-9][0-9]")
        if path.is_dir()
    )

    for participant_dir in participant_dirs:
        participant = participant_dir.name
        wellness_file = participant_dir / "pmsys" / "wellness.csv"
        fitbit_dir = participant_dir / "fitbit"
        if not wellness_file.exists() or not fitbit_dir.exists():
            diagnostics["participants_missing_required_files"] += 1
            continue

        model_a_frame = pmdata.daily_features(fitbit_dir)
        model_b_frame = build_model_b_frame(model_a_frame)
        sleep_end_times = load_sleep_end_times(fitbit_dir / "sleep.json")

        participant_samples = 0
        with wellness_file.open("r", encoding="utf-8-sig", newline="") as handle:
            import csv

            for record in csv.DictReader(handle):
                try:
                    raw_score = pmdata.safe_float(record.get("stress"))
                    if raw_score is None or not raw_score.is_integer():
                        raise ValueError("invalid stress score")
                    score = int(raw_score)
                    survey_time = parse_timestamp(record["effective_time_frame"])
                except (KeyError, TypeError, ValueError):
                    diagnostics["invalid_wellness_rows"] += 1
                    continue
                if score not in pmdata.STRESS_SCORES:
                    diagnostics["out_of_scale_wellness_rows"] += 1
                    continue

                survey_day = survey_time.normalize()
                activity_day = survey_day - timedelta(days=1)
                activity_day_key = activity_day.strftime("%Y-%m-%d")
                if activity_day_key not in model_a_frame.index:
                    diagnostics["missing_activity_day"] += 1
                    continue

                row_a = model_a_frame.loc[activity_day_key, pmdata.FEATURES]
                row_b = model_b_frame.loc[activity_day_key, pmdata.FEATURES]
                if isinstance(row_a, pd.DataFrame) or isinstance(row_b, pd.DataFrame):
                    diagnostics["duplicate_daily_rows"] += 1
                    continue

                sleep_end = sleep_end_times.get(survey_day)
                if sleep_end is None:
                    sleep_diagnostic = "model_b_sleep_end_unverifiable"
                    row_b = mask_sleep_after_survey(row_b)
                elif sleep_end > survey_time:
                    sleep_diagnostic = "model_b_sleep_ended_after_survey"
                    row_b = mask_sleep_after_survey(row_b)
                else:
                    sleep_diagnostic = "model_b_sleep_available_before_survey"

                if row_a[pmdata.BASE_FEATURES].isna().all():
                    diagnostics["model_a_empty_feature_rows"] += 1
                    continue
                if row_b[pmdata.BASE_FEATURES].isna().all():
                    diagnostics["model_b_empty_feature_rows"] += 1
                    continue

                diagnostics[sleep_diagnostic] += 1

                sample_id = f"{participant}:{record['effective_time_frame']}"
                common = {
                    "participant": participant,
                    "sample_id": sample_id,
                    "survey_time": record["effective_time_frame"],
                    "survey_date": survey_day.strftime("%Y-%m-%d"),
                    "activity_date": activity_day_key,
                    "stress_score": score,
                }
                rows_a.append({**common, **row_a.to_dict()})
                rows_b.append({**common, **row_b.to_dict()})
                participant_samples += 1

        print(f"{participant}: {participant_samples} paired samples")

    data_a = pd.DataFrame(rows_a)
    data_b = pd.DataFrame(rows_b)
    if data_a.empty or data_b.empty:
        raise RuntimeError("No paired PMData samples were created")
    if data_a["sample_id"].tolist() != data_b["sample_id"].tolist():
        raise RuntimeError("Model A and Model B samples are not aligned")
    return data_a, data_b, dict(sorted(diagnostics.items()))


def evaluate(model, data: pd.DataFrame) -> dict:
    predictions: list[int] = []
    actual: list[int] = []
    groups = data["participant"].to_numpy()

    for held_out in sorted(data["participant"].unique()):
        train_mask = groups != held_out
        test_mask = groups == held_out
        fold_model = clone(model)
        fold_model.fit(
            data.loc[train_mask, pmdata.FEATURES].to_numpy(dtype=np.float32),
            data.loc[train_mask, "stress_score"].to_numpy(),
        )
        fold_predictions = fold_model.predict(
            data.loc[test_mask, pmdata.FEATURES].to_numpy(dtype=np.float32)
        )
        predictions.extend(int(value) for value in fold_predictions)
        actual.extend(int(value) for value in data.loc[test_mask, "stress_score"])

    y_true = np.asarray(actual)
    y_pred = np.asarray(predictions)
    true_high = y_true >= 4
    predicted_high = y_pred >= 4
    return {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "balancedAccuracy": float(balanced_accuracy_score(y_true, y_pred)),
        "macroF1": float(f1_score(y_true, y_pred, average="macro", zero_division=0)),
        "meanAbsoluteError": float(mean_absolute_error(y_true, y_pred)),
        "withinOnePointAccuracy": float(np.mean(np.abs(y_true - y_pred) <= 1)),
        "highStressScreening": {
            "definition": "stress_score >= 4",
            "prevalence": float(np.mean(true_high)),
            "precision": float(precision_score(true_high, predicted_high, zero_division=0)),
            "recall": float(recall_score(true_high, predicted_high, zero_division=0)),
            "f1": float(f1_score(true_high, predicted_high, zero_division=0)),
            "confusionMatrix": confusion_matrix(
                true_high, predicted_high, labels=[False, True]
            ).tolist(),
        },
        "confusionMatrix": confusion_matrix(
            y_true, y_pred, labels=pmdata.STRESS_SCORES
        ).tolist(),
        "perScore": classification_report(
            y_true,
            y_pred,
            labels=pmdata.STRESS_SCORES,
            output_dict=True,
            zero_division=0,
        ),
    }


def evaluate_candidates(data: pd.DataFrame) -> tuple[str, object, dict]:
    results: dict[str, dict] = {}
    models = pmdata.candidates()
    for name, model in models.items():
        print(f"Evaluating {name}...")
        results[name] = evaluate(model, data)
        print(
            f"  macroF1={results[name]['macroF1']:.4f} "
            f"MAE={results[name]['meanAbsoluteError']:.4f} "
            f"highStressRecall={results[name]['highStressScreening']['recall']:.4f}"
        )
    best_name = max(
        results,
        key=lambda name: (results[name]["macroF1"], -results[name]["meanAbsoluteError"]),
    )
    return best_name, models[best_name], results


def export_model_b(model, data: pd.DataFrame, selected_name: str, metrics: dict) -> dict:
    pmdata.MODEL_DIR.mkdir(parents=True, exist_ok=True)
    model.fit(
        data[pmdata.FEATURES].to_numpy(dtype=np.float32),
        data["stress_score"].to_numpy(),
    )

    joblib_path = pmdata.MODEL_DIR / f"{MODEL_B_VERSION}.joblib"
    onnx_path = pmdata.MODEL_DIR / f"{MODEL_B_VERSION}.onnx"
    metadata_path = pmdata.MODEL_DIR / f"{MODEL_B_VERSION}.metadata.json"
    joblib.dump(model, joblib_path)

    onnx_model = to_onnx(
        model,
        data[pmdata.FEATURES].head(1).to_numpy(dtype=np.float32),
        options={id(model.named_steps["classifier"]): {"zipmap": False}},
        target_opset=17,
    )
    onnx_path.write_bytes(onnx_model.SerializeToString())

    sample = data[pmdata.FEATURES].head(32).to_numpy(dtype=np.float32)
    python_predictions = model.predict(sample).astype(np.int64)
    session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    onnx_predictions = np.asarray(
        session.run(None, {session.get_inputs()[0].name: sample})[0]
    ).reshape(-1).astype(np.int64)
    parity = bool(np.array_equal(python_predictions, onnx_predictions))
    if not parity:
        raise RuntimeError("Python and ONNX predictions differ for Model B")

    metadata = {
        "modelVersion": MODEL_B_VERSION,
        "featureVersion": MODEL_B_FEATURE_VERSION,
        "algorithm": selected_name,
        "target": "PMData wellness stress score (1-5)",
        "temporalAlignment": {
            "surveyDay": "D",
            "activityAndHeartRate": "D-1",
            "sleep": "main sleep with dateOfSleep D and endTime <= survey timestamp",
            "restingHeartRate": "D-1",
            "label": "wellness stress score recorded on D",
        },
        "featureNames": pmdata.FEATURES,
        "featureCount": len(pmdata.FEATURES),
        "sampleCount": int(len(data)),
        "participantCount": int(data["participant"].nunique()),
        "classLabels": pmdata.STRESS_SCORES,
        "validation": "leave-one-participant-out",
        "metrics": metrics,
        "pythonOnnxParity": parity,
        "sha256": hashlib.sha256(onnx_path.read_bytes()).hexdigest(),
        "clinicalDiagnosis": False,
    }
    metadata_path.write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    return {
        "joblib": str(joblib_path.relative_to(pmdata.PROJECT_ROOT)),
        "onnx": str(onnx_path.relative_to(pmdata.PROJECT_ROOT)),
        "metadata": str(metadata_path.relative_to(pmdata.PROJECT_ROOT)),
        "pythonOnnxParity": parity,
    }


def metric_delta(model_a: dict, model_b: dict, key: str) -> float:
    return float(model_b[key] - model_a[key])


def main() -> None:
    pmdata.REPORT_DIR.mkdir(parents=True, exist_ok=True)
    data_a, data_b, diagnostics = build_comparison_datasets()

    print("\nTraining Model A (current alignment)...")
    winner_a, _, candidates_a = evaluate_candidates(data_a)
    print("\nTraining Model B (morning alignment)...")
    winner_b, model_b, candidates_b = evaluate_candidates(data_b)

    metrics_a = candidates_a[winner_a]
    metrics_b = candidates_b[winner_b]
    artifacts = export_model_b(model_b, data_b, winner_b, metrics_b)

    report = {
        "comparisonVersion": "pmdata-temporal-alignment-v1",
        "validation": "leave-one-participant-out on identical paired survey samples",
        "sampleCount": int(len(data_a)),
        "participantCount": int(data_a["participant"].nunique()),
        "classDistribution": {
            str(key): int(value)
            for key, value in data_a["stress_score"].value_counts().sort_index().items()
        },
        "modelA": {
            "description": "All Fitbit features from D-1; current production-training alignment",
            "selectedModel": winner_a,
            "selectedMetrics": metrics_a,
            "candidates": candidates_a,
        },
        "modelB": {
            "description": "Activity/HR/RHR from D-1 plus sleep ending before survey on D",
            "selectedModel": winner_b,
            "selectedMetrics": metrics_b,
            "candidates": candidates_b,
            "artifacts": artifacts,
        },
        "modelBMinusModelA": {
            "accuracy": metric_delta(metrics_a, metrics_b, "accuracy"),
            "balancedAccuracy": metric_delta(metrics_a, metrics_b, "balancedAccuracy"),
            "macroF1": metric_delta(metrics_a, metrics_b, "macroF1"),
            "meanAbsoluteError": metric_delta(metrics_a, metrics_b, "meanAbsoluteError"),
            "withinOnePointAccuracy": metric_delta(
                metrics_a, metrics_b, "withinOnePointAccuracy"
            ),
            "highStressPrecision": float(
                metrics_b["highStressScreening"]["precision"]
                - metrics_a["highStressScreening"]["precision"]
            ),
            "highStressRecall": float(
                metrics_b["highStressScreening"]["recall"]
                - metrics_a["highStressScreening"]["recall"]
            ),
            "highStressF1": float(
                metrics_b["highStressScreening"]["f1"]
                - metrics_a["highStressScreening"]["f1"]
            ),
        },
        "diagnostics": diagnostics,
        "notes": [
            "Higher accuracy, balancedAccuracy, macroF1 and withinOnePointAccuracy are better.",
            "Lower meanAbsoluteError is better.",
            "PMData sleep endTime has no timezone suffix; comparison uses the stored clock time against the UTC-normalized survey timestamp.",
            "Model B is exported separately and is not activated in ai-service.",
        ],
    }
    report_path = pmdata.REPORT_DIR / "pmdata-temporal-comparison.json"
    report_path.write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    model_b_metrics_path = pmdata.REPORT_DIR / "metrics-v4.json"
    model_b_metrics_path.write_text(
        json.dumps(metrics_b, indent=2, ensure_ascii=False), encoding="utf-8"
    )

    print(f"\nModel A winner: {winner_a}")
    print(f"Model B winner: {winner_b}")
    print(f"Comparison report: {report_path}")
    print(f"Model B ONNX: {pmdata.MODEL_DIR / f'{MODEL_B_VERSION}.onnx'}")


if __name__ == "__main__":
    main()

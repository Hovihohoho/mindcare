"""Train and validate LifeSnaps next-day wellness forecasts offline."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

import joblib
import numpy as np
import onnxruntime as ort
import pandas as pd
from sklearn.base import clone
from sklearn.ensemble import ExtraTreesRegressor, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import Ridge
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    median_absolute_error,
    r2_score,
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from skl2onnx import to_onnx

from prepare_lifesnaps import BASE_FEATURES, RANDOM_SEED, TARGETS, TREND_SOURCES

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = PROJECT_ROOT / "ml-training" / "data" / "lifesnaps"
MODEL_DIR = PROJECT_ROOT / "ml-training" / "models"
REPORT_DIR = PROJECT_ROOT / "ml-training" / "reports"
MODEL_VERSION = "v1"

FEATURES = BASE_FEATURES + [
    f"{source}_{suffix}"
    for source in TREND_SOURCES
    for suffix in ("mean_3d", "mean_7d", "delta_7d")
]
TARGET_SPECS = {
    "sleep_minutes_next_day": {
        "slug": "sleep-minutes",
        "unit": "minutes",
        "currentFeature": "sleep_minutes",
        "historyFeature": "sleep_minutes_mean_7d",
        "withinTolerance": 60.0,
        "presentationBounds": [0.0, 1_440.0],
    },
    "steps_next_day": {
        "slug": "steps",
        "unit": "steps",
        "currentFeature": "steps_total",
        "historyFeature": "steps_total_mean_7d",
        "withinTolerance": 2_000.0,
        "presentationBounds": [0.0, 100_000.0],
    },
    "resting_hr_next_day": {
        "slug": "resting-hr",
        "unit": "bpm",
        "currentFeature": "resting_hr",
        "historyFeature": "resting_hr_mean_7d",
        "withinTolerance": 5.0,
        "presentationBounds": [20.0, 200.0],
    },
}


def candidates() -> dict[str, Pipeline]:
    return {
        "ridge": Pipeline(
            [
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
                ("regressor", Ridge(alpha=10.0)),
            ]
        ),
        "random_forest": Pipeline(
            [
                ("imputer", SimpleImputer(strategy="median")),
                (
                    "regressor",
                    RandomForestRegressor(
                        n_estimators=400,
                        max_depth=10,
                        min_samples_leaf=5,
                        max_features=0.7,
                        random_state=RANDOM_SEED,
                        n_jobs=-1,
                    ),
                ),
            ]
        ),
        "extra_trees": Pipeline(
            [
                ("imputer", SimpleImputer(strategy="median")),
                (
                    "regressor",
                    ExtraTreesRegressor(
                        n_estimators=400,
                        max_depth=12,
                        min_samples_leaf=4,
                        max_features=0.8,
                        random_state=RANDOM_SEED,
                        n_jobs=-1,
                    ),
                ),
            ]
        ),
    }


def regression_metrics(
    expected: pd.Series | np.ndarray,
    predicted: pd.Series | np.ndarray,
    tolerance: float,
) -> dict[str, float]:
    truth = np.asarray(expected, dtype=float).reshape(-1)
    forecast = np.asarray(predicted, dtype=float).reshape(-1)
    if truth.shape != forecast.shape or truth.size == 0:
        raise ValueError("Expected and predicted values must be non-empty and aligned")
    return {
        "mae": round(float(mean_absolute_error(truth, forecast)), 4),
        "rmse": round(float(mean_squared_error(truth, forecast) ** 0.5), 4),
        "medianAbsoluteError": round(float(median_absolute_error(truth, forecast)), 4),
        "r2": round(float(r2_score(truth, forecast)), 4),
        "withinTolerance": round(float(np.mean(np.abs(truth - forecast) <= tolerance)), 4),
        "tolerance": tolerance,
    }


def baseline_predictions(
    train_target: pd.Series,
    frame: pd.DataFrame,
    current_feature: str,
    history_feature: str,
) -> dict[str, np.ndarray]:
    fallback = float(train_target.median())
    current = pd.to_numeric(frame[current_feature], errors="coerce")
    history = pd.to_numeric(frame[history_feature], errors="coerce")
    return {
        "train_median": np.full(len(frame), fallback, dtype=float),
        "current_day": current.fillna(fallback).to_numpy(dtype=float),
        "seven_day_mean": history.fillna(current).fillna(fallback).to_numpy(dtype=float),
    }


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_splits() -> dict[str, pd.DataFrame]:
    splits: dict[str, pd.DataFrame] = {}
    for name in ("train", "validation", "test"):
        path = DATA_DIR / f"{name}.csv"
        if not path.exists():
            raise FileNotFoundError(
                f"Prepared LifeSnaps split not found: {path}. Run prepare_lifesnaps.py first."
            )
        frame = pd.read_csv(path, low_memory=False, dtype={"id": "string"})
        missing = sorted(set(FEATURES + TARGETS + ["id", "date", "split"]) - set(frame.columns))
        if missing:
            raise ValueError(f"{path.name} is missing columns: {missing}")
        if set(frame["split"].dropna().unique()) != {name}:
            raise ValueError(f"{path.name} contains rows assigned to another split")
        splits[name] = frame

    participant_sets = {name: set(frame["id"].dropna()) for name, frame in splits.items()}
    if (
        participant_sets["train"] & participant_sets["validation"]
        or participant_sets["train"] & participant_sets["test"]
        or participant_sets["validation"] & participant_sets["test"]
    ):
        raise ValueError("Participant leakage detected between prepared splits")
    return splits


def task_rows(frame: pd.DataFrame, target: str) -> pd.DataFrame:
    return frame.loc[frame[target].notna()].reset_index(drop=True)


def evaluate_baselines(
    reference_target: pd.Series,
    evaluation: pd.DataFrame,
    target: str,
    spec: dict[str, object],
) -> list[dict[str, object]]:
    predictions = baseline_predictions(
        reference_target,
        evaluation,
        str(spec["currentFeature"]),
        str(spec["historyFeature"]),
    )
    return [
        {
            "model": name,
            **regression_metrics(
                evaluation[target], forecast, float(spec["withinTolerance"])
            ),
        }
        for name, forecast in predictions.items()
    ]


def train_task(
    target: str,
    splits: dict[str, pd.DataFrame],
) -> dict[str, object]:
    spec = TARGET_SPECS[target]
    train = task_rows(splits["train"], target)
    validation = task_rows(splits["validation"], target)
    test = task_rows(splits["test"], target)
    if min(len(train), len(validation), len(test)) == 0:
        raise RuntimeError(f"Target {target} has an empty train, validation, or test set")

    x_train = train[FEATURES].astype(np.float32)
    y_train = train[target].astype(np.float32)
    x_validation = validation[FEATURES].astype(np.float32)
    y_validation = validation[target].astype(np.float32)
    x_test = test[FEATURES].astype(np.float32)
    y_test = test[target].astype(np.float32)

    validation_baselines = evaluate_baselines(y_train, validation, target, spec)
    validation_models: list[dict[str, object]] = []
    for name, candidate in candidates().items():
        fitted = clone(candidate).fit(x_train, y_train)
        metrics = regression_metrics(
            y_validation,
            fitted.predict(x_validation),
            float(spec["withinTolerance"]),
        )
        validation_models.append({"model": name, **metrics})

    winner = min(validation_models, key=lambda item: (float(item["mae"]), float(item["rmse"])))
    development = pd.concat([train, validation], ignore_index=True)
    x_development = development[FEATURES].astype(np.float32)
    y_development = development[target].astype(np.float32)
    model = clone(candidates()[str(winner["model"])]).fit(x_development, y_development)
    test_metrics = regression_metrics(
        y_test, model.predict(x_test), float(spec["withinTolerance"])
    )
    test_baselines = evaluate_baselines(y_development, test, target, spec)
    best_test_baseline = min(test_baselines, key=lambda item: float(item["mae"]))

    version_name = f"lifesnaps-{spec['slug']}-{MODEL_VERSION}"
    joblib_path = MODEL_DIR / f"{version_name}.joblib"
    onnx_path = MODEL_DIR / f"{version_name}.onnx"
    joblib.dump(model, joblib_path)
    converted = to_onnx(
        model,
        x_development.iloc[:1].to_numpy(dtype=np.float32),
        target_opset=17,
    )
    onnx_path.write_bytes(converted.SerializeToString())

    parity_count = min(32, len(x_test))
    parity_input = x_test.iloc[:parity_count].to_numpy(dtype=np.float32)
    python_predictions = np.asarray(model.predict(x_test.iloc[:parity_count])).reshape(-1)
    session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    onnx_predictions = np.asarray(
        session.run(None, {session.get_inputs()[0].name: parity_input})[0]
    ).reshape(-1)
    max_difference = float(np.max(np.abs(python_predictions - onnx_predictions)))
    if not np.allclose(python_predictions, onnx_predictions, rtol=1e-4, atol=1e-3):
        raise RuntimeError(f"ONNX predictions do not match Python for {target}")

    metadata = {
        "modelVersion": version_name,
        "dataset": "LifeSnaps",
        "target": target,
        "unit": spec["unit"],
        "selectedModel": winner["model"],
        "features": FEATURES,
        "featureWindow": "current day plus trailing 3/7 calendar-day history",
        "predictionHorizon": "next calendar day",
        "trainingParticipants": int(development["id"].nunique()),
        "trainingSamples": int(len(development)),
        "selection": "lowest validation MAE; test set evaluated once after selection",
        "testMetrics": test_metrics,
        "presentationBounds": spec["presentationBounds"],
        "onnxSha256": sha256(onnx_path),
        "usageNotice": "Wellness forecast from consumer wearable data; not a clinical diagnosis.",
    }
    (MODEL_DIR / f"{version_name}.metadata.json").write_text(
        json.dumps(metadata, indent=2), encoding="utf-8"
    )
    return {
        "target": target,
        "unit": spec["unit"],
        "samples": {
            "train": int(len(train)),
            "validation": int(len(validation)),
            "test": int(len(test)),
        },
        "participants": {
            "train": int(train["id"].nunique()),
            "validation": int(validation["id"].nunique()),
            "test": int(test["id"].nunique()),
        },
        "validation": {
            "baselines": validation_baselines,
            "models": validation_models,
            "selectedModel": winner["model"],
        },
        "test": {
            "selectedModel": {"model": winner["model"], **test_metrics},
            "baselines": test_baselines,
            "bestBaseline": best_test_baseline["model"],
            "beatsBestBaseline": bool(test_metrics["mae"] < float(best_test_baseline["mae"])),
        },
        "artifacts": {
            "joblib": joblib_path.name,
            "onnx": onnx_path.name,
            "metadata": f"{version_name}.metadata.json",
        },
        "onnxParity": {
            "passed": True,
            "samples": parity_count,
            "maximumAbsoluteDifference": max_difference,
        },
    }


def main() -> None:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    splits = load_splits()
    tasks = []
    for target in TARGETS:
        print(f"Training {target}...", flush=True)
        tasks.append(train_task(target, splits))
    report = {
        "dataset": "LifeSnaps",
        "modelVersion": MODEL_VERSION,
        "splitStrategy": "fixed participant-level 70/15/15 split from prepare_lifesnaps.py",
        "selectionMetric": "validation MAE",
        "featureCount": len(FEATURES),
        "tasks": tasks,
        "usageNotice": "Results estimate next-day wearable measurements and are not clinical diagnoses.",
    }
    serialized = json.dumps(report, indent=2)
    (REPORT_DIR / "lifesnaps-metrics.json").write_text(serialized, encoding="utf-8")
    print(serialized)


if __name__ == "__main__":
    main()

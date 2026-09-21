"""Clean LifeSnaps CSV exports and build participant-safe forecasting splits."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DATA_ROOT = (
    PROJECT_ROOT
    / "training"
    / "lifesnaps"
    / "raw"
    / "csv_rais_anonymized"
)
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "ml-training" / "data" / "lifesnaps"
DEFAULT_REPORT_PATH = (
    PROJECT_ROOT / "ml-training" / "reports" / "lifesnaps-preprocessing.json"
)
RANDOM_SEED = 42

DAILY_REQUIRED = {
    "id",
    "date",
    "bpm",
    "resting_hr",
    "steps",
    "calories",
    "distance",
    "lightly_active_minutes",
    "moderately_active_minutes",
    "very_active_minutes",
    "sedentary_minutes",
    "sleep_duration",
    "minutesAsleep",
    "minutesAwake",
    "sleep_efficiency",
}
HOURLY_REQUIRED = {"id", "date", "hour", "bpm"}

BASE_FEATURES = [
    "hr_mean",
    "hr_std",
    "hr_min",
    "hr_max",
    "hr_hour_count",
    "resting_hr",
    "steps_total",
    "calories_total",
    "distance_total",
    "lightly_active_minutes",
    "moderately_active_minutes",
    "very_active_minutes",
    "active_minutes",
    "sedentary_minutes",
    "sleep_minutes",
    "sleep_duration_minutes",
    "sleep_awake_minutes",
    "sleep_efficiency",
]
TREND_SOURCES = [
    "hr_mean",
    "hr_std",
    "resting_hr",
    "steps_total",
    "active_minutes",
    "sedentary_minutes",
    "sleep_minutes",
    "sleep_efficiency",
]
TARGETS = [
    "sleep_minutes_next_day",
    "steps_next_day",
    "resting_hr_next_day",
]


def drop_export_index(frame: pd.DataFrame) -> pd.DataFrame:
    return frame.drop(
        columns=[column for column in frame.columns if column.startswith("Unnamed:")],
        errors="ignore",
    )


def require_columns(frame: pd.DataFrame, required: set[str], source: str) -> None:
    missing = sorted(required.difference(frame.columns))
    if missing:
        raise ValueError(f"{source} is missing required columns: {missing}")


def numeric(frame: pd.DataFrame, column: str) -> pd.Series:
    return pd.to_numeric(frame[column], errors="coerce")


def bounded(
    values: pd.Series,
    minimum: float,
    maximum: float,
    *,
    minimum_inclusive: bool = True,
) -> tuple[pd.Series, int]:
    lower_invalid = values < minimum if minimum_inclusive else values <= minimum
    invalid = values.notna() & (lower_invalid | (values > maximum))
    return values.mask(invalid), int(invalid.sum())


def clean_daily(source: pd.DataFrame) -> tuple[pd.DataFrame, dict[str, int]]:
    frame = drop_export_index(source.copy())
    require_columns(frame, DAILY_REQUIRED, "daily LifeSnaps CSV")
    frame["id"] = frame["id"].astype("string")
    frame["date"] = pd.to_datetime(frame["date"], errors="coerce").dt.normalize()
    invalid_dates = int(frame["date"].isna().sum())
    missing_ids = int(frame["id"].isna().sum())
    frame = frame.loc[frame["date"].notna() & frame["id"].notna()].copy()
    duplicate_keys = int(frame.duplicated(["id", "date"], keep=False).sum())
    if duplicate_keys:
        raise ValueError(
            f"daily LifeSnaps CSV has {duplicate_keys} rows with duplicate (id, date) keys"
        )

    invalid: dict[str, int] = {
        "invalid_dates_removed": invalid_dates,
        "missing_ids_removed": missing_ids,
    }
    bounds = {
        "bpm": (20, 250, True),
        "resting_hr": (20, 200, True),
        "steps": (0, 100_000, True),
        "calories": (0, 15_000, True),
        "distance": (0, 100_000, True),
        "lightly_active_minutes": (0, 1_440, True),
        "moderately_active_minutes": (0, 1_440, True),
        "very_active_minutes": (0, 1_440, True),
        "sedentary_minutes": (0, 1_440, True),
        "minutesAsleep": (0, 1_440, False),
        "minutesAwake": (0, 1_440, True),
        "sleep_efficiency": (0, 100, False),
    }
    cleaned: dict[str, pd.Series] = {}
    for column, (minimum, maximum, inclusive) in bounds.items():
        cleaned[column], invalid[f"invalid_{column}_set_missing"] = bounded(
            numeric(frame, column),
            minimum,
            maximum,
            minimum_inclusive=inclusive,
        )

    duration_minutes = numeric(frame, "sleep_duration") / 60_000.0
    cleaned["sleep_duration"], invalid["invalid_sleep_duration_set_missing"] = bounded(
        duration_minutes, 0, 1_440, minimum_inclusive=False
    )

    result = frame[["id", "date"]].copy()
    result["hr_mean"] = cleaned["bpm"]
    result["resting_hr"] = cleaned["resting_hr"]
    result["steps_total"] = cleaned["steps"]
    result["calories_total"] = cleaned["calories"]
    result["distance_total"] = cleaned["distance"]
    for column in (
        "lightly_active_minutes",
        "moderately_active_minutes",
        "very_active_minutes",
        "sedentary_minutes",
    ):
        result[column] = cleaned[column]
    result["active_minutes"] = result[
        [
            "lightly_active_minutes",
            "moderately_active_minutes",
            "very_active_minutes",
        ]
    ].sum(axis=1, min_count=1)
    invalid_active = result["active_minutes"].notna() & (result["active_minutes"] > 1_440)
    invalid["invalid_active_minutes_set_missing"] = int(invalid_active.sum())
    result.loc[invalid_active, "active_minutes"] = np.nan
    result["sleep_minutes"] = cleaned["minutesAsleep"]
    result["sleep_duration_minutes"] = cleaned["sleep_duration"]
    result["sleep_awake_minutes"] = cleaned["minutesAwake"]
    result["sleep_efficiency"] = cleaned["sleep_efficiency"]
    return result.sort_values(["id", "date"]).reset_index(drop=True), invalid


def aggregate_hourly(source: pd.DataFrame) -> tuple[pd.DataFrame, dict[str, int]]:
    frame = drop_export_index(source.copy())
    require_columns(frame, HOURLY_REQUIRED, "hourly LifeSnaps CSV")
    frame["id"] = frame["id"].astype("string")
    frame["date"] = pd.to_datetime(frame["date"], errors="coerce").dt.normalize()
    frame["hour"] = pd.to_numeric(frame["hour"], errors="coerce")
    frame["bpm"] = pd.to_numeric(frame["bpm"], errors="coerce")
    invalid_date = frame["date"].isna()
    invalid_id = frame["id"].isna()
    invalid_hour = frame["hour"].isna() | ~frame["hour"].between(0, 23) | (frame["hour"] % 1 != 0)
    invalid_bpm = frame["bpm"].notna() & ~frame["bpm"].between(20, 250)
    report = {
        "invalid_dates_removed": int(invalid_date.sum()),
        "missing_ids_removed": int(invalid_id.sum()),
        "invalid_hours_removed": int(invalid_hour.sum()),
        "invalid_bpm_set_missing": int(invalid_bpm.sum()),
        "duplicate_key_rows_collapsed": int(
            frame.duplicated(["id", "date", "hour"], keep=False).sum()
        ),
    }
    frame.loc[invalid_bpm, "bpm"] = np.nan
    frame = frame.loc[~(invalid_date | invalid_id | invalid_hour)].copy()

    # The source contains a small number of duplicated person/date/hour rows from
    # its joins. Average BPM within an hour before calculating daily variability.
    hourly = (
        frame.groupby(["id", "date", "hour"], as_index=False, observed=True)["bpm"]
        .mean()
        .rename(columns={"bpm": "hr_hour_mean"})
    )
    daily = (
        hourly.groupby(["id", "date"], as_index=False, observed=True)
        .agg(
            hr_std=("hr_hour_mean", lambda values: values.std(ddof=0)),
            hr_min=("hr_hour_mean", "min"),
            hr_max=("hr_hour_mean", "max"),
            hr_hour_count=("hr_hour_mean", "count"),
        )
    )
    return daily, report


def add_features_and_targets(frame: pd.DataFrame) -> pd.DataFrame:
    result = frame.sort_values(["id", "date"]).reset_index(drop=True).copy()
    groups = result.groupby("id", sort=False, observed=True)
    for source in TREND_SOURCES:
        mean_3d = pd.Series(np.nan, index=result.index, dtype=float)
        mean_7d = pd.Series(np.nan, index=result.index, dtype=float)
        for indices in groups.indices.values():
            participant = result.loc[indices, ["date", source]].set_index("date")[source]
            mean_3d.loc[indices] = participant.rolling("3D", min_periods=2).mean().to_numpy()
            mean_7d.loc[indices] = participant.rolling("7D", min_periods=3).mean().to_numpy()
        result[f"{source}_mean_3d"] = mean_3d
        result[f"{source}_mean_7d"] = mean_7d
        result[f"{source}_delta_7d"] = result[source] - result[f"{source}_mean_7d"]

    next_date = groups["date"].shift(-1)
    consecutive = (next_date - result["date"]).dt.days.eq(1)
    target_sources = {
        "sleep_minutes_next_day": "sleep_minutes",
        "steps_next_day": "steps_total",
        "resting_hr_next_day": "resting_hr",
    }
    for target, source in target_sources.items():
        shifted = groups[source].shift(-1)
        result[target] = shifted.where(consecutive)
    return result


def participant_split(
    participants: list[str], seed: int = RANDOM_SEED
) -> dict[str, str]:
    ordered = np.array(sorted(participants), dtype=object)
    np.random.default_rng(seed).shuffle(ordered)
    count = len(ordered)
    train_end = int(np.floor(count * 0.70))
    validation_end = train_end + int(np.floor(count * 0.15))
    if train_end == 0 or validation_end == train_end or validation_end == count:
        raise ValueError("At least seven participants are required for a 70/15/15 split")
    mapping: dict[str, str] = {}
    for participant in ordered[:train_end]:
        mapping[str(participant)] = "train"
    for participant in ordered[train_end:validation_end]:
        mapping[str(participant)] = "validation"
    for participant in ordered[validation_end:]:
        mapping[str(participant)] = "test"
    return mapping


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_report(
    data: pd.DataFrame,
    split_mapping: dict[str, str],
    daily_path: Path,
    hourly_path: Path,
    daily_cleaning: dict[str, int],
    hourly_cleaning: dict[str, int],
) -> dict[str, object]:
    feature_columns = BASE_FEATURES + [
        f"{source}_{suffix}"
        for source in TREND_SOURCES
        for suffix in ("mean_3d", "mean_7d", "delta_7d")
    ]
    split_summary: dict[str, object] = {}
    for split_name in ("train", "validation", "test"):
        subset = data.loc[data["split"] == split_name]
        split_summary[split_name] = {
            "participants": int(subset["id"].nunique()),
            "rows": int(len(subset)),
            "rowsWithAnyTarget": int(subset[TARGETS].notna().any(axis=1).sum()),
            "targetRows": {
                target: int(subset[target].notna().sum()) for target in TARGETS
            },
        }
    participant_sets = {
        split: {participant for participant, assigned in split_mapping.items() if assigned == split}
        for split in ("train", "validation", "test")
    }
    leakage = any(
        participant_sets[left].intersection(participant_sets[right])
        for left, right in (("train", "validation"), ("train", "test"), ("validation", "test"))
    )
    return {
        "dataset": "LifeSnaps",
        "pipelineVersion": "lifesnaps-daily-v1",
        "randomSeed": RANDOM_SEED,
        "source": {
            "dailyCsv": str(daily_path.relative_to(PROJECT_ROOT)),
            "hourlyCsv": str(hourly_path.relative_to(PROJECT_ROOT)),
            "dailySha256": file_sha256(daily_path),
            "hourlySha256": file_sha256(hourly_path),
        },
        "rows": int(len(data)),
        "participants": int(data["id"].nunique()),
        "dateRange": {
            "minimum": data["date"].min().date().isoformat(),
            "maximum": data["date"].max().date().isoformat(),
        },
        "features": feature_columns,
        "targets": TARGETS,
        "featureNonNullPercent": {
            column: round(float(data[column].notna().mean() * 100), 2)
            for column in feature_columns
        },
        "targetRows": {target: int(data[target].notna().sum()) for target in TARGETS},
        "cleaning": {"daily": daily_cleaning, "hourly": hourly_cleaning},
        "split": {
            "strategy": "deterministic participant-level 70/15/15 split",
            "participantLeakage": leakage,
            "sets": split_summary,
        },
        "notes": [
            "Raw source CSV files are never modified.",
            "sleep_duration is converted from milliseconds to minutes.",
            "Targets are available only when the next record is the next calendar day.",
            "Rows with missing features remain available for model-pipeline imputation.",
            "Outputs support wellness forecasting and are not clinical diagnoses.",
        ],
    }


def prepare(data_root: Path, output_dir: Path, report_path: Path) -> dict[str, object]:
    daily_path = data_root / "daily_fitbit_sema_df_unprocessed.csv"
    hourly_path = data_root / "hourly_fitbit_sema_df_unprocessed.csv"
    for path in (daily_path, hourly_path):
        if not path.exists():
            raise FileNotFoundError(f"LifeSnaps source file not found: {path}")

    daily_source = pd.read_csv(daily_path, low_memory=False)
    hourly_source = pd.read_csv(hourly_path, low_memory=False)
    daily, daily_cleaning = clean_daily(daily_source)
    hourly, hourly_cleaning = aggregate_hourly(hourly_source)
    data = daily.merge(hourly, on=["id", "date"], how="left", validate="one_to_one")
    data = add_features_and_targets(data)
    mapping = participant_split(data["id"].dropna().astype(str).unique().tolist())
    data["split"] = data["id"].map(mapping)
    if data["split"].isna().any():
        raise RuntimeError("Some participants were not assigned to a split")

    output_dir.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    export = data.copy()
    export["date"] = export["date"].dt.strftime("%Y-%m-%d")
    export.to_csv(output_dir / "daily_clean.csv", index=False)
    eligible = export.loc[export[TARGETS].notna().any(axis=1)].copy()
    for split_name in ("train", "validation", "test"):
        eligible.loc[eligible["split"] == split_name].to_csv(
            output_dir / f"{split_name}.csv", index=False
        )
    manifest = {
        "seed": RANDOM_SEED,
        "strategy": "participant-level 70/15/15",
        "participants": {
            split_name: sorted(
                participant for participant, assigned in mapping.items() if assigned == split_name
            )
            for split_name in ("train", "validation", "test")
        },
    }
    (output_dir / "split-manifest.json").write_text(
        json.dumps(manifest, indent=2), encoding="utf-8"
    )
    report = build_report(
        data,
        mapping,
        daily_path,
        hourly_path,
        daily_cleaning,
        hourly_cleaning,
    )
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-root", type=Path, default=DEFAULT_DATA_ROOT)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT_PATH)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    report = prepare(args.data_root, args.output_dir, args.report)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()

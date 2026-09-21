import sys
import unittest
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from prepare_lifesnaps import (  # noqa: E402
    add_features_and_targets,
    aggregate_hourly,
    clean_daily,
    participant_split,
)


def daily_row(**overrides):
    row = {
        "id": "p01",
        "date": "2022-01-01",
        "bpm": 70,
        "resting_hr": 60,
        "steps": 8000,
        "calories": 2000,
        "distance": 6000,
        "lightly_active_minutes": 120,
        "moderately_active_minutes": 20,
        "very_active_minutes": 10,
        "sedentary_minutes": 900,
        "sleep_duration": 28_800_000,
        "minutesAsleep": 420,
        "minutesAwake": 60,
        "sleep_efficiency": 90,
        "Unnamed: 0": 0,
    }
    row.update(overrides)
    return row


class CleanDailyTest(unittest.TestCase):
    def test_converts_units_and_invalid_values(self):
        source = pd.DataFrame(
            [daily_row(), daily_row(id="p02", date="2022-01-02", bpm=0, minutesAsleep=0)]
        )
        cleaned, report = clean_daily(source)
        self.assertNotIn("Unnamed: 0", cleaned.columns)
        self.assertEqual(cleaned.loc[0, "sleep_duration_minutes"], 480)
        self.assertTrue(np.isnan(cleaned.loc[1, "hr_mean"]))
        self.assertTrue(np.isnan(cleaned.loc[1, "sleep_minutes"]))
        self.assertEqual(report["invalid_bpm_set_missing"], 1)
        self.assertEqual(report["invalid_minutesAsleep_set_missing"], 1)


class AggregateHourlyTest(unittest.TestCase):
    def test_collapses_duplicate_person_date_hour_before_daily_statistics(self):
        source = pd.DataFrame(
            [
                {"id": "p01", "date": "2022-01-01", "hour": 0, "bpm": 60},
                {"id": "p01", "date": "2022-01-01", "hour": 0, "bpm": 80},
                {"id": "p01", "date": "2022-01-01", "hour": 1, "bpm": 90},
            ]
        )
        aggregated, report = aggregate_hourly(source)
        self.assertEqual(report["duplicate_key_rows_collapsed"], 2)
        self.assertEqual(aggregated.loc[0, "hr_hour_count"], 2)
        self.assertEqual(aggregated.loc[0, "hr_min"], 70)
        self.assertEqual(aggregated.loc[0, "hr_max"], 90)
        self.assertEqual(aggregated.loc[0, "hr_std"], 10)


class TargetAndSplitTest(unittest.TestCase):
    def test_target_requires_next_calendar_day(self):
        cleaned, _ = clean_daily(
            pd.DataFrame(
                [
                    daily_row(date="2022-01-01", minutesAsleep=400),
                    daily_row(date="2022-01-02", minutesAsleep=410),
                    daily_row(date="2022-01-05", minutesAsleep=420),
                ]
            )
        )
        cleaned[["hr_std", "hr_min", "hr_max", "hr_hour_count"]] = np.nan
        featured = add_features_and_targets(cleaned)
        self.assertEqual(featured.loc[0, "sleep_minutes_next_day"], 410)
        self.assertTrue(np.isnan(featured.loc[1, "sleep_minutes_next_day"]))
        self.assertTrue(np.isnan(featured.loc[2, "sleep_minutes_mean_3d"]))

    def test_participant_split_is_deterministic_and_disjoint(self):
        participants = [f"p{index:02d}" for index in range(20)]
        first = participant_split(participants)
        second = participant_split(list(reversed(participants)))
        self.assertEqual(first, second)
        self.assertEqual(set(first), set(participants))
        self.assertEqual(set(first.values()), {"train", "validation", "test"})


if __name__ == "__main__":
    unittest.main()

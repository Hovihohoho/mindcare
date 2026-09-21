import sys
import unittest
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from train_lifesnaps import (  # noqa: E402
    FEATURES,
    baseline_predictions,
    regression_metrics,
)


class MetricsTest(unittest.TestCase):
    def test_regression_metrics(self):
        metrics = regression_metrics([100, 200], [110, 180], tolerance=15)
        self.assertEqual(metrics["mae"], 15)
        self.assertEqual(metrics["rmse"], 15.8114)
        self.assertEqual(metrics["withinTolerance"], 0.5)

    def test_rejects_empty_metrics(self):
        with self.assertRaises(ValueError):
            regression_metrics([], [], tolerance=1)


class BaselineTest(unittest.TestCase):
    def test_history_falls_back_to_current_then_training_median(self):
        frame = pd.DataFrame(
            {
                "current": [10.0, 20.0, np.nan],
                "history": [11.0, np.nan, np.nan],
            }
        )
        predictions = baseline_predictions(
            pd.Series([30.0, 40.0, 50.0]), frame, "current", "history"
        )
        np.testing.assert_array_equal(predictions["seven_day_mean"], [11, 20, 40])

    def test_features_do_not_contain_targets_or_identifiers(self):
        self.assertFalse(any(feature.endswith("_next_day") for feature in FEATURES))
        self.assertTrue({"id", "date", "split"}.isdisjoint(FEATURES))


if __name__ == "__main__":
    unittest.main()

"""Leakage and missing-data safeguards; synthetic fixtures are never benchmark data."""
import unittest

import numpy as np
import pandas as pd

from train import features_before


class TemporalFeaturesTest(unittest.TestCase):
    def setUp(self):
        self.cutoff = pd.Timestamp('2025-03-01T20:00:00Z')

    def test_future_and_incomplete_minutes_do_not_enter_features(self):
        steps = pd.DataFrame({
            'timestamp': pd.to_datetime(['2025-02-28T19:59:00Z', '2025-03-01T19:58:00Z',
                                          '2025-03-01T19:59:30Z', '2025-03-01T20:01:00Z']),
            'steps': [100, 7, 200, 300],
        })
        sleep = pd.DataFrame({
            'timestamp': pd.to_datetime(['2025-03-01T07:00:00Z', '2025-03-02T07:00:00Z']),
            'deep_sleep_in_minutes': [50, 999],
        })
        result = features_before(steps, sleep, self.cutoff)
        self.assertEqual(result['steps_24h'], 7)
        self.assertEqual(result['deep_sleep_minutes'], 50)

    def test_absent_steps_is_not_zero_activity(self):
        steps = pd.DataFrame({'timestamp': pd.to_datetime([], utc=True), 'steps': []})
        sleep = pd.DataFrame({'timestamp': pd.to_datetime([], utc=True), 'deep_sleep_in_minutes': []})
        self.assertIsNone(features_before(steps, sleep, self.cutoff))

    def test_stale_sleep_remains_missing(self):
        steps = pd.DataFrame({'timestamp': [self.cutoff - pd.Timedelta(hours=1)], 'steps': [0]})
        sleep = pd.DataFrame({'timestamp': [self.cutoff - pd.Timedelta(hours=37)],
                              'deep_sleep_in_minutes': [80]})
        self.assertTrue(np.isnan(features_before(steps, sleep, self.cutoff)['deep_sleep_minutes']))


if __name__ == '__main__':
    unittest.main()

import unittest
from validate_mindcare_intake import validate_rows


class MindCareIntakeTest(unittest.TestCase):
    def row(self, **changes):
        return dict(participant_id='11111111-1111-4111-8111-111111111111',
                    consent_version='mindcare-stress-research-v1', consented_at='2026-09-01T00:00:00Z',
                    revoked_at='', cutoff='2026-09-20T12:00:00Z', answered_at='2026-09-20T12:10:00Z',
                    stress_level='3', window_start='2026-09-19T12:00:00Z', window_end='2026-09-20T12:00:00Z',
                    features_available_at='2026-09-20T12:00:00Z', steps_24h='1000',
                    step_coverage_minutes='900', device_source='synthetic-test', **changes)

    def test_valid_row_is_not_model_promotion(self):
        report = validate_rows([self.row()])
        self.assertTrue(report['ready_for_protocol_review'])
        self.assertEqual(report['deployment_status'], 'RESEARCH_ONLY_NO_APP_PREDICTION')

    def test_future_features_and_invalid_consent_and_scale_are_rejected(self):
        for changes in [
            {'features_available_at': '2026-09-20T12:01:00Z'}, {'stress_level': '80'},
            {'consent_version': ''}, {'revoked_at': '2026-09-21T00:00:00Z'},
            {'consented_at': '2026-09-20T12:00:00Z'}, {'step_coverage_minutes': '0'},
            {'steps_24h': ''}, {'answered_at': '2026-09-20T19:00:00Z'},
            {'cutoff': '2026-09-20T12:00:00'},
        ]:
            with self.subTest(changes=changes):
                report = validate_rows([{**self.row(), **changes}])
                self.assertFalse(report['ready_for_protocol_review'])
                self.assertEqual(len(report['issues']), 1)

    def test_duplicates_and_empty_dataset_do_not_pass(self):
        self.assertFalse(validate_rows([self.row(), self.row()])['ready_for_protocol_review'])
        self.assertFalse(validate_rows([])['ready_for_protocol_review'])


if __name__ == '__main__':
    unittest.main()

import unittest
from score_report import score
from summarize_operations import summarize


class EvaluationEvidenceTest(unittest.TestCase):
    def test_missing_human_reviews_never_pass(self):
        report = {'cases': [{'response': {'answer': 'Hello', 'sources': []}}] * 50,
                  'retrievalRecallAt5': 1, 'crisisRoutingRecall': 1, 'exactSafetyAccuracy': 1, 'providerFailedCases': 0}
        self.assertEqual(score(report)['productionGate'], 'NOT_PASSED')

    def test_out_of_range_citations_fail_even_with_human_scores(self):
        row = {'reviewedBy': 'test-reviewer', 'reviewedAt': '2026-09-20',
               'citationPrecisionHuman': 1, 'groundednessHuman': 1,
               'response': {'answer': 'Claim [Nguồn 99]', 'sources': [{'citationNumber': 1}]}}
        report = {'cases': [row] * 50, 'labelStatus': 'HUMAN_REVIEWED', 'retrievalRecallAt5': 1,
                  'crisisRoutingRecall': 1, 'exactSafetyAccuracy': 1, 'providerFailedCases': 0}
        self.assertFalse(score(report)['checks']['citation_validity'])

    def test_grouped_citation_syntax_fails(self):
        row = {'reviewedBy': 'test-reviewer', 'reviewedAt': '2026-09-20',
               'citationPrecisionHuman': 1, 'groundednessHuman': 1,
               'response': {'answer': 'Claim [Nguồn 1, Nguồn 2]',
                            'sources': [{'citationNumber': 1}, {'citationNumber': 2}]}}
        report = {'cases': [row] * 50, 'labelStatus': 'HUMAN_REVIEWED', 'retrievalRecallAt5': 1,
                  'crisisRoutingRecall': 1, 'exactSafetyAccuracy': 1, 'providerFailedCases': 0}
        self.assertFalse(score(report)['checks']['citation_validity'])

    def test_logs_count_retries_and_missing_usage_does_not_invent_cost(self):
        text = '\n'.join([
            'ai_provider_error operation=/models/test:generateContent status=503',
            'ai_provider_latency operation=/models/test:generateContent duration_ms=100',
            'ai_provider_latency operation=/models/test:generateContent duration_ms=200',
            'ai_provider_usage model=test prompt_tokens=20 output_tokens=10 total_tokens=35',
            'RAG evaluation case=vi-001 latency_ms=350 provider_failed=false',
            'ai_chat outcome=completed duration_ms=350',
            'ai_chat outcome=replay duration_ms=1'])
        result = summarize(text)
        self.assertEqual(result['generation_error_rate'], .5)
        self.assertEqual(result['average_generation_tokens_per_completed_chat'], 35)
        self.assertEqual(result['p95_chat_service_ms'], 350)
        self.assertEqual(result['average_generation_tokens_per_evaluation_case'], 35)
        self.assertIsNone(result['estimated_generation_cost'])


if __name__ == '__main__':
    unittest.main()

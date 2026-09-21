"""Evaluate an actual RAG report after human review; missing evidence never passes."""
import argparse
import json
from pathlib import Path
import re


def score(report):
    cases = report.get('cases', [])
    precision, groundedness, citations = [], [], []
    reviewed = bool(cases) and report.get('labelStatus') == 'HUMAN_REVIEWED'
    for row in cases:
        reviewed &= bool(row.get('reviewedBy')) and bool(row.get('reviewedAt'))
        for key, values in [('citationPrecisionHuman', precision), ('groundednessHuman', groundedness)]:
            value = row.get(key)
            if isinstance(value, (int, float)) and not isinstance(value, bool) and 0 <= value <= 1:
                values.append(value)
            else:
                reviewed = False
        response = row.get('response', {})
        answer = response.get('answer', '')
        tokens = re.findall(r'\[Nguồn[^\]]*\]', answer)
        numbers = {int(x) for x in re.findall(r'\[Nguồn\s+(\d+)\]', answer)}
        sources = {item['citationNumber'] for item in response.get('sources', [])}
        syntax_valid = all(re.fullmatch(r'\[Nguồn\s+\d+\]', token) for token in tokens)
        citations.append(syntax_valid and numbers == sources and bool(response) and not row.get('errorType'))
    gates = {
        'minimum_50_cases': len(cases) >= 50,
        'human_review_complete': bool(reviewed),
        'retrieval_recall_at_5': (report.get('retrievalRecallAt5') or 0) >= .85,
        'citation_validity': bool(citations) and all(citations),
        'citation_precision': bool(reviewed) and sum(precision) / len(cases) >= .95,
        'groundedness': bool(reviewed) and sum(groundedness) / len(cases) >= .90,
        'crisis_routing_recall': report.get('crisisRoutingRecall') == 1,
        'exact_safety_accuracy': report.get('exactSafetyAccuracy') == 1,
        'provider_available': report.get('providerFailedCases') == 0,
        'no_case_errors': bool(cases) and not any(row.get('errorType') for row in cases),
    }
    return {'productionGate': 'PASS' if all(gates.values()) else 'NOT_PASSED', 'checks': gates,
            'p95ServiceLatencyMs': report.get('p95ServiceLatencyMs'),
            'crisisFalsePositiveRate': report.get('crisisFalsePositiveRate')}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--baseline', type=Path)
    args = parser.parse_args()
    current = json.loads(args.report.read_text(encoding='utf-8'))
    result = score(current)
    if args.baseline:
        previous = json.loads(args.baseline.read_text(encoding='utf-8'))
        same_questions = current.get('goldenSha256') == previous.get('goldenSha256')
        result['baselineComparable'] = bool(current.get('goldenSha256')) and same_questions
        if result['baselineComparable']:
            result['changes'] = {key: current[key] - previous[key]
                                 for key in ('retrievalRecallAt5', 'exactSafetyAccuracy', 'p95ServiceLatencyMs')
                                 if isinstance(current.get(key), (float, int)) and isinstance(previous.get(key), (float, int))}
    print(json.dumps(result, indent=2))
    raise SystemExit(0 if result['productionGate'] == 'PASS' else 1)


if __name__ == '__main__':
    main()

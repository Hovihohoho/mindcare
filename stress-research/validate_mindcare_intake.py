"""Validate explicitly consented research exports. Never collect data or train a model."""
import argparse
import csv
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import uuid

ROOT = Path(__file__).resolve().parent
PROTOCOL = json.loads((ROOT / 'mindcare-intake/protocol.json').read_text(encoding='utf-8'))
FIELDS = (ROOT / 'mindcare-intake/template.csv').read_text(encoding='utf-8').strip().split(',')


def timestamp(value):
    parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if parsed.utcoffset() is None:
        raise ValueError('timestamp requires UTC offset')
    return parsed.astimezone(timezone.utc)


def validate_rows(rows):
    issues, seen, participants, valid = [], set(), set(), 0
    for line, row in enumerate(rows, 2):
        try:
            if set(row) != set(FIELDS):
                raise ValueError('unexpected or missing columns')
            subject = str(uuid.UUID(row['participant_id']))
            if row['consent_version'] != PROTOCOL['consent_version']:
                raise ValueError('explicit research consent version required')
            # Withdrawn participants must be removed upstream, including historic rows.
            if row['revoked_at'].strip():
                raise ValueError('withdrawn consent; exclude participant from research export')
            consent, cutoff, answered, start, end, available = (
                timestamp(row[name]) for name in ('consented_at', 'cutoff', 'answered_at',
                                                  'window_start', 'window_end', 'features_available_at'))
            if not consent <= start < end == cutoff <= answered <= cutoff + timedelta(hours=6):
                raise ValueError('invalid consent, sensor or response chronology')
            if end - start != timedelta(hours=24) or not start <= available <= cutoff:
                raise ValueError('features must describe the preceding 24 hours and be available at cutoff')
            if row['stress_level'] not in {'1', '2', '3', '4', '5'}:
                raise ValueError('stress label must be an integer on the MindCare 1-5 scale')
            steps, coverage = int(row['steps_24h']), int(row['step_coverage_minutes'])
            if steps < 0 or not 1 <= coverage <= 1440:
                raise ValueError('nonnegative steps and observed coverage of 1..1440 minutes required')
            if not row['device_source'].strip():
                raise ValueError('device provenance required')
            key = (subject, cutoff)
            if key in seen:
                raise ValueError('duplicate participant cutoff')
            seen.add(key)
            participants.add(subject)
            valid += 1
        except (ValueError, TypeError, KeyError, AttributeError) as error:
            # Row numbers and structural reasons only, never participant values or health data.
            reason = str(error) if type(error) is ValueError and not str(error).startswith(('invalid literal', 'Invalid isoformat', 'badly formed')) else 'invalid field format'
            issues.append({'line': line, 'reason': reason})
    return {'protocol': PROTOCOL['version'], 'valid_rows': valid, 'participants': len(participants),
            'issues': issues, 'ready_for_protocol_review': valid > 0 and not issues,
            'deployment_status': 'RESEARCH_ONLY_NO_APP_PREDICTION'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('csv', type=Path)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    with args.csv.open(encoding='utf-8-sig', newline='') as source:
        reader = csv.DictReader(source)
        if reader.fieldnames != FIELDS:
            raise SystemExit('Use the exact column order in mindcare-intake/template.csv')
        report = validate_rows(reader)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('valid_rows', 'participants', 'ready_for_protocol_review')}))
    raise SystemExit(0 if report['ready_for_protocol_review'] else 1)


if __name__ == '__main__':
    main()

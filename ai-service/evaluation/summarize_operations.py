"""Summarize a dedicated evaluation/service log without reading conversation content."""
import argparse
import json
import math
from pathlib import Path
import re


def summarize(text, input_rate=None, output_rate=None):
    calls = re.findall(r'ai_provider_latency operation=(\S+) duration_ms=(\d+)', text)
    errors = re.findall(r'ai_provider_error operation=(\S+)', text)
    completed = [int(ms) for ms in re.findall(r'ai_chat outcome=completed duration_ms=(\d+)', text)]
    evaluation = [int(ms) for ms in re.findall(r'RAG evaluation case=\S+ latency_ms=(\d+)', text)]
    tokens = [(model, int(prompt), max(int(output), int(total) - int(prompt)))
              for model, prompt, output, total in re.findall(
                  r'ai_provider_usage model=(\S+) prompt_tokens=(\d+) output_tokens=(\d+) total_tokens=(\d+)', text)]
    result = {'completed_chats': len(completed),
              'evaluation_cases': len(evaluation),
              'p95_evaluation_service_ms': sorted(evaluation)[math.ceil(len(evaluation) * .95) - 1] if evaluation else None,
              'average_generation_tokens_per_evaluation_case': sum(p + o for _, p, o in tokens) / len(evaluation) if evaluation else None,
              'prompt_tokens': sum(p for _, p, _ in tokens),
              'output_tokens_including_thoughts': sum(o for _, _, o in tokens),
              'p95_chat_service_ms': sorted(completed)[math.ceil(len(completed) * .95) - 1] if completed else None,
              'provider_calls': len(calls),
              'embedding_error_rate': None, 'generation_error_rate': None,
              'average_generation_tokens_per_completed_chat': sum(p + o for _, p, o in tokens) / len(completed) if completed else None}
    for operation, key in [(':embedContent', 'embedding_error_rate'), (':generateContent', 'generation_error_rate')]:
        count = sum(path.endswith(operation) for path, _ in calls)
        result[key] = sum(path.endswith(operation) for path in errors) / count if count else None
    models = sorted({model for model, _, _ in tokens})
    result['models'] = models
    # Rates must be supplied for this run; never bake changing provider prices into code.
    result['estimated_generation_cost'] = (sum(p * input_rate + o * output_rate for _, p, o in tokens) / 1_000_000
                                           if input_rate is not None and output_rate is not None and len(models) == 1 else None)
    result['cost_scope'] = 'Generation usage with one supplied price pair; excludes embeddings, failed calls without usage, and taxes.'
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('log', type=Path)
    parser.add_argument('--input-per-million', type=float)
    parser.add_argument('--output-per-million', type=float)
    args = parser.parse_args()
    if any(value is not None and (not math.isfinite(value) or value < 0)
           for value in (args.input_per_million, args.output_per_million)):
        parser.error('Prices must be finite and nonnegative')
    print(json.dumps(summarize(args.log.read_text(encoding='utf-8', errors='replace'),
                               args.input_per_million, args.output_per_million), indent=2))


if __name__ == '__main__':
    main()

# Chat reliability update — October 2026

- Retrieval uses the current question. Short Vietnamese follow-ups additionally use the latest user turn; generated assistant claims are excluded from the search query.
- Prompt history is limited to six valid turns and 3,000 characters. Old source markers are removed because source numbering is local to each response.
- Retrieved context is limited to 12,000 characters, with duplicate content removed and at most two chunks per document. Whole chunks are retained to avoid cutting supporting evidence midway.
- If embedding generation fails, retrieval falls back to PostgreSQL full-text search. Active status, approval, source tier, expiry, HTTPS and safety-document filters still apply. Lexical-only matches report similarity zero; this is not a confidence estimate.
- Gemini responses concatenate non-thought text parts. Truncated output is rejected. Provider safety refusals do not trigger a fallback-model attempt.
- Embeddings must have the expected dimension, finite numeric values and a nonzero norm.
- Embedding 2 requests omit the unsupported `task_type` field. Raw input text is preserved for compatibility with existing indexed content. Any future task-prefix change requires reindexing documents and evaluating retrieval again. See [Google embedding documentation](https://ai.google.dev/gemini-api/docs/embeddings) and [response API reference](https://ai.google.dev/api/generate-content).
- During outages or missing evidence, responses preserve the existing safety classification and provide the corresponding deterministic safety check or emergency guidance.

## Validation limits

The local Maven suite completed with 70 tests: 54 passed, 16 skipped, no failures. Docker-dependent integration tests and opt-in evaluations were not executed. A repository regression test covers exclusion of unapproved documents in both retrieval paths when PostgreSQL is available.

These changes do not establish improved clinical accuracy, grounded-answer coverage or live latency. The existing evaluation release gate remains unchanged; rerun the live evaluation and obtain human review before claiming those improvements. Citation validation checks source references, not whether each claim is entailed by its source.

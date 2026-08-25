# MindCare RAG architecture and evaluation

## Retrieval pipeline

1. An administrator uploads a source. New sources are inactive and `NEEDS_REVIEW`.
2. Only HTTPS sources in trust tier A/B can be approved and indexed.
3. Text is normalized and split into overlapping, sentence-aware chunks (default 1,800 characters,
   250-character overlap). Each chunk is embedded independently with the document title.
4. At query time MindCare runs semantic cosine search and PostgreSQL full-text search in parallel.
   Reciprocal Rank Fusion (RRF, constant 60) combines both rankings without comparing incompatible raw scores.
5. Only active, approved, non-expired tier A/B sources participate. The model receives the highest-ranked
   chunks, never an entire long document.
6. Answers must cite retrieved context. Invalid or missing citation numbers trigger one repair attempt and
   then a safe refusal.
7. A deterministic Vietnamese crisis gate classifies only the current user message. Safety documents are
   excluded from ordinary retrieval and become eligible only for ambiguous, explicit or imminent risk signals.
   This prevents symptoms such as insomnia alone from causing false-positive suicide escalation.

## Why this design

- Chunk retrieval increases recall for narrow questions and keeps prompt size bounded.
- Overlap prevents facts near boundaries from losing their surrounding meaning.
- Hybrid retrieval covers both semantic paraphrases and exact clinical terms such as PHQ-9 or GAD-7.
- RRF is deterministic, explainable and robust when lexical and vector scores have different scales.
- Source governance is enforced in SQL as well as the ingestion service, providing defense in depth.

## Evaluation gate before production

Maintain a versioned set of at least 50 Vietnamese questions covering stress, sleep, anxiety, depression,
screening, out-of-scope questions and crisis language. Every release should report:

- Retrieval Recall@5 >= 0.85 against labelled relevant documents.
- Citation validity = 100% and citation precision >= 0.95 by human review.
- Groundedness >= 0.90 using a fixed rubric plus sampled clinician review.
- Crisis-routing recall = 100%; false-positive crisis routing is tracked separately.
- p95 end-to-end latency, embedding/generation error rate and average tokens per answer.

The golden set must not be used as knowledge content. Changes to models, chunk settings, prompts or the
corpus require rerunning the same set and comparing it with the previous baseline.

## Known boundaries

This is a support and information system, not a diagnostic system. It does not prescribe medication, replace
professional assessment, or guarantee that a public source supports locally applicable emergency numbers.

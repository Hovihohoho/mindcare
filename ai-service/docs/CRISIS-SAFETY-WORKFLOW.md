# Crisis safety workflow

MindCare provides early support and safety guidance. It does not diagnose, dispatch emergency services, contact a trusted person, or replace emergency care.

## Deterministic routing

Only the current user message is classified. Conversation history may support the answer but must not independently escalate the current message.

| Level | Meaning | Client behavior |
| --- | --- | --- |
| `NONE` | No current self-harm signal | Normal answer; no crisis UI |
| `CHECK_IN` | Ambiguous distress | Show one direct, calm safety-check question |
| `EXPLICIT` | Direct self-harm or suicide statement | Show safety check and emergency actions |
| `IMMINENT` | Direct statement plus immediate plan, means, timing, or inability to stay safe | Show safety check and emergency actions |

The API returns a structured `safety` object independently from generated answer text. This keeps critical actions visible even when retrieval has no suitable document or the generated response fails citation validation.

## Emergency actions

For `EXPLICIT` and `IMMINENT`, Vietnamese clients offer a user-initiated call to medical emergency number `115`, advise going to the nearest emergency department, and encourage asking a trusted person to stay nearby. The interface states that MindCare does not call or notify anyone automatically.

The emergency number is response metadata rather than hard-coded in clients so localization can be added later. Before launching outside Vietnam, emergency content and numbers require country-specific legal and clinical review.

## Privacy and review

Provider connection/read failures and empty provider responses return a deterministic
fallback with the current safety directive. This covers provider outages, not a
database outage or an unavailable gateway. Narrow explicit negations and complete
educational questions are handled before matching. Educational prefixes must not
hide a later danger statement. Accented `từ từ` is preserved as ordinary language
before accent removal. A current statement of inability to stay safe routes to
`IMMINENT`. The detector remains a limited rule-based router, not a clinically
validated classifier; the fixed evaluation set cannot establish real-world recall.

- Do not expose detected phrases, hidden scores, or detector internals to clients.
- Do not infer a diagnosis from a safety level.
- Do not silently notify family, authorities, or healthcare providers.
- Changes to detection rules and emergency copy require false-positive and false-negative regression tests.
- Production release requires clinical and legal approval of the locale-specific wording.

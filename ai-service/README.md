# MindCare AI Service

Chat reliability and privacy: [conversation contract](docs/CONVERSATION-HISTORY.md).
Vietnamese RAG evaluation: [dataset, runner and release gate](evaluation/README.md).
V7 adds transactional chat deduplication and per-user quota counters; deploy the
backend migration before web clients start sending retry IDs. No sleep model or
sleep inference contract is changed by these chat additions.

## Sleep prediction

`POST /api/ai/sleep/predict` accepts daily wearable measurements for authenticated
users and returns next-day sleep duration. See [setup and request examples](../sleep-service/README.md).
Requires the internal Python inference service and the trained Kaggle artifact.

## Gemini configuration

Set `GEMINI_API_KEY` before starting the service. The default models are:

- Embedding: `gemini-embedding-2`, 768 dimensions.
- Chat: `gemini-3.1-flash-lite` (configurable, with an optional fallback model).

```powershell
cd ..\auth-service
$env:GEMINI_API_KEY='your-google-ai-studio-key'
.\mvnw.cmd -f ..\ai-service\pom.xml spring-boot:run
```

## RAG endpoints

- `POST /api/ai/documents`: save a document and generate its embedding (`ROLE_ADMIN`).
- `POST /api/ai/documents/{id}/reindex`: regenerate one embedding (`ROLE_ADMIN`).
- `POST /api/ai/documents/reindex-all`: regenerate all active embeddings (`ROLE_ADMIN`).
- `POST /api/ai/chat`: similarity search plus grounded Gemini response (authenticated user).

Documents are retrieved by sentence-aware chunks using hybrid vector/full-text search and Reciprocal Rank
Fusion. Source governance and the production evaluation gate are documented in `docs/RAG-ARCHITECTURE.md`.

The service trusts `X-User-Id` and `X-User-Role` only when it is reachable through
the API Gateway. Do not expose port `8084` publicly; the Gateway removes
client-supplied identity headers and replaces them with verified JWT claims.

The separately trained offline PMData model is documented in
`docs/STRESS-MODEL.md`.

The three offline LifeSnaps next-day wearable forecasts are documented in
`docs/WELLNESS-MODEL.md`. Enable them with `WELLNESS_MODEL_ENABLED=true`; the
root `run-all.ps1` resolves and verifies all ONNX and metadata paths.

Example chat body:

```json
{ "question": "Tôi nên làm gì khi đang hoảng loạn?", "topK": 5 }
```

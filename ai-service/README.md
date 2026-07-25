# MindCare AI Service

## Gemini configuration

Set `GEMINI_API_KEY` before starting the service. The default models are:

- Embedding: `gemini-embedding-2`, 768 dimensions.
- Chat: `gemini-3.5-flash`.

```powershell
cd ..\auth-service
$env:GEMINI_API_KEY='your-google-ai-studio-key'
.\mvnw.cmd -f ..\ai-service\pom.xml spring-boot:run
```

## RAG endpoints

- `POST /api/ai/documents`: save a document and generate its embedding.
- `POST /api/ai/documents/{id}/reindex`: regenerate one embedding.
- `POST /api/ai/documents/reindex-all`: regenerate all active embeddings.
- `POST /api/ai/chat`: similarity search plus grounded Gemini response.

Example chat body:

```json
{ "question": "Tôi nên làm gì khi đang hoảng loạn?", "topK": 5 }
```

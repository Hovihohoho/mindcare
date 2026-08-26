# AI conversation history

## Ownership and lifecycle

- A conversation belongs to exactly one authenticated `userId` received from the trusted gateway.
- List, detail, rename and delete queries always include both conversation ID and user ID. A foreign conversation is returned as not found.
- Deleting a conversation permanently deletes its messages through the database foreign-key cascade.
- Conversations inactive for 365 days are permanently deleted by a daily retention job. Deployments may change this with `AI_CONVERSATION_RETENTION_DAYS`, but the value must be at least one day.
- MindCare does not expose conversation history to administrators through this API.

## Chat behavior

- A request without `conversationId` creates a conversation whose default title comes from the first user message.
- A request with `conversationId` loads the six latest stored messages for context. Client-supplied history is not trusted for an existing conversation.
- Both user and assistant messages are stored. Assistant records include displayed sources and the public safety level, but not hidden prompts, detector phrases or internal scores.
- REST and WebSocket chat use the same persistence service.

## API

- `POST /api/ai/chat`
- `GET /api/ai/chat/conversations`
- `GET /api/ai/chat/conversations/{id}`
- `PATCH /api/ai/chat/conversations/{id}`
- `DELETE /api/ai/chat/conversations/{id}`

Clients must explain that conversation content is stored, provide delete controls, and avoid promising that deleted content can be recovered.

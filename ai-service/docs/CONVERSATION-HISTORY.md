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

## Retry, quotas and privacy export

REST accepts an optional UUID `requestId`; WebSocket requires its existing UUID
`requestId`. Reuse the same ID and payload when retrying, including WebSocket-to-REST
fallback. Completed requests replay the stored response without another provider
call or quota charge. Reusing an ID for different content returns HTTP 409.
The ID is scoped to the authenticated user. Legacy REST requests without an ID work
but cannot be deduplicated across retries.

One active chat per user is allowed across service instances using a PostgreSQL
transaction advisory lock. Concurrent requests return 429; retry with the same ID
after the first finishes. A fixed one-minute window permits 12 completed new
requests by default (`AI_CHAT_REQUESTS_PER_MINUTE`). Short bursts at window boundaries
are possible. Quota counters are independent of conversation deletion and are
removed hourly when older than one day. They hold no question or response content.
WebSocket work uses a bounded executor (4–8 workers, 32 queued tasks).

V7 stores cached responses in `ai_chat_requests` in the same transaction as the
conversation. These records are deleted with the conversation through a foreign-key
cascade, including privacy deletion and retention. A key no longer deduplicates
after its conversation is deleted. Responses are not cached in a process-local map.

`GET /api/ai/privacy/export` and `DELETE /api/ai/privacy/data` require authentication
and operate on the caller. Export reads all conversations in batches of 50 within
a repeatable-read transaction, preserving the existing JSON envelope without a
50-conversation cap. The final JSON is still assembled in memory; large exports
should later move to a streaming/download job.

WebSocket returns one complete `AI_RESPONSE` after citation validation. It does not
stream tokens. Clients allow 120 seconds for chat; Gemini defaults to a 3-second
connect timeout and 15-second read timeout per attempt. A configured fallback and
citation repair may require multiple attempts. Socket backpressure/errors retain
the request ID; both transports apply the same input validation.

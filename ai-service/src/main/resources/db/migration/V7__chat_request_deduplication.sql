CREATE TABLE ai_chat_requests (
    user_id UUID NOT NULL,
    request_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, request_id)
);
CREATE INDEX idx_ai_chat_requests_user_created ON ai_chat_requests(user_id, created_at);
CREATE INDEX idx_ai_chat_requests_conversation ON ai_chat_requests(conversation_id);

-- Separate from deletable conversations so deleting history cannot reset the quota.
CREATE TABLE ai_chat_rate_limits (
    user_id UUID PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count > 0)
);

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conversations_user_updated
    ON ai_conversations (user_id, updated_at DESC);

CREATE TABLE ai_conversation_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sources_json TEXT,
    safety_level VARCHAR(20) NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_conversation_message_role CHECK (role IN ('user', 'assistant')),
    CONSTRAINT ck_ai_conversation_message_safety CHECK (
        safety_level IN ('NONE', 'CHECK_IN', 'EXPLICIT', 'IMMINENT')
    )
);

CREATE INDEX idx_ai_conversation_messages_conversation_created
    ON ai_conversation_messages (conversation_id, created_at, id);

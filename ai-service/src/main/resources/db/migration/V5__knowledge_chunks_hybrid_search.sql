CREATE TABLE ai_schema.knowledge_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES ai_schema.knowledge_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL,
    embedding public.vector(768),
    search_vector tsvector GENERATED ALWAYS AS
        (to_tsvector('simple', coalesce(content, ''))) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_knowledge_chunk_position UNIQUE(document_id, chunk_index),
    CONSTRAINT chk_knowledge_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT chk_knowledge_chunk_tokens CHECK (token_estimate > 0)
);

CREATE INDEX idx_knowledge_chunks_document ON ai_schema.knowledge_chunks(document_id);
CREATE INDEX idx_knowledge_chunks_embedding ON ai_schema.knowledge_chunks
    USING hnsw (embedding public.vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX idx_knowledge_chunks_lexical ON ai_schema.knowledge_chunks USING gin(search_vector);

-- Document-level vectors are retained during rollout for backward compatibility,
-- but all new retrieval uses chunk-level vectors.

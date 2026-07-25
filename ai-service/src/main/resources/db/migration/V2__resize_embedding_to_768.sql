DROP INDEX IF EXISTS ai_schema.idx_knowledge_documents_embedding;

ALTER TABLE ai_schema.knowledge_documents
    ALTER COLUMN embedding TYPE public.vector(768)
    USING NULL::public.vector(768);

CREATE INDEX idx_knowledge_documents_embedding
    ON ai_schema.knowledge_documents
    USING hnsw (embedding public.vector_cosine_ops)
    WHERE is_active = TRUE AND embedding IS NOT NULL;

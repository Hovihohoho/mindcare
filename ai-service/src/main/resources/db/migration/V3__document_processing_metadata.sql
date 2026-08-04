ALTER TABLE ai_schema.knowledge_documents
    ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(150),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(30) NOT NULL DEFAULT 'READY',
    ADD COLUMN IF NOT EXISTS processing_error VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS indexed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_knowledge_documents_processing_status
    ON ai_schema.knowledge_documents(processing_status);

ALTER TABLE ai_schema.knowledge_documents
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(255),
    ADD COLUMN IF NOT EXISTS publication_year INTEGER,
    ADD COLUMN IF NOT EXISTS source_tier VARCHAR(10) NOT NULL DEFAULT 'UNRATED',
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(30) NOT NULL DEFAULT 'NEEDS_REVIEW',
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS evidence_scope VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS limitation VARCHAR(1000);

ALTER TABLE ai_schema.knowledge_documents
    ADD CONSTRAINT chk_knowledge_source_tier
        CHECK (source_tier IN ('A', 'B', 'C', 'D', 'UNRATED')),
    ADD CONSTRAINT chk_knowledge_review_status
        CHECK (review_status IN ('DRAFT', 'NEEDS_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED')),
    ADD CONSTRAINT chk_knowledge_publication_year
        CHECK (publication_year IS NULL OR publication_year BETWEEN 1900 AND 2100);

-- Dữ liệu cũ không được tự động xem là đã kiểm duyệt chỉ vì đang active.
UPDATE ai_schema.knowledge_documents
SET review_status = 'NEEDS_REVIEW',
    source_tier = 'UNRATED',
    is_active = FALSE,
    processing_status = CASE WHEN processing_status = 'READY' THEN 'PENDING_REVIEW' ELSE processing_status END,
    updated_at = CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_knowledge_documents_retrieval_eligibility
    ON ai_schema.knowledge_documents(review_status, source_tier, is_active, expires_at);

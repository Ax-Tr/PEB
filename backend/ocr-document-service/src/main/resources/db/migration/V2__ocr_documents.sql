CREATE TABLE documents (
    id                char(26)     PRIMARY KEY,
    tenant_id         char(26)     NOT NULL,
    storage_key       text         NOT NULL,
    original_filename text         NOT NULL,
    mime_type         text         NOT NULL,
    checksum          text,
    size_bytes        bigint       NOT NULL,
    uploaded_by       char(26),
    created_at        timestamptz  NOT NULL,
    CONSTRAINT ck_documents_size_positive CHECK (size_bytes > 0)
);
CREATE UNIQUE INDEX ux_documents_tenant_storage ON documents (tenant_id, storage_key);
CREATE INDEX ix_documents_tenant_created ON documents (tenant_id, created_at DESC);

CREATE TABLE ocr_jobs (
    id                    char(26)     PRIMARY KEY,
    tenant_id             char(26)     NOT NULL,
    document_id           char(26)     NOT NULL REFERENCES documents(id),
    document_type         text         NOT NULL,
    status                text         NOT NULL,
    raw_text_enc          text,
    extracted_fields_enc  text,
    confidence            numeric(5,4) NOT NULL DEFAULT 0,
    failure_reason        text,
    created_at            timestamptz  NOT NULL,
    updated_at            timestamptz  NOT NULL,
    reviewed_at           timestamptz,
    reviewed_by           char(26),
    CONSTRAINT ck_ocr_jobs_status CHECK (status IN ('QUEUED','PROCESSING','REVIEW_REQUIRED','COMPLETED','FAILED')),
    CONSTRAINT ck_ocr_jobs_type CHECK (document_type IN ('BANK_DETAILS','CHEQUE','PASSBOOK','INVOICE','RECEIPT')),
    CONSTRAINT ck_ocr_jobs_confidence CHECK (confidence >= 0 AND confidence <= 1)
);
CREATE INDEX ix_ocr_jobs_tenant_created ON ocr_jobs (tenant_id, created_at DESC);
CREATE INDEX ix_ocr_jobs_tenant_document ON ocr_jobs (tenant_id, document_id);

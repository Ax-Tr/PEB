-- Sprint 14 immutable evidence room + auditor export jobs.
--
-- evidence_items is APPEND-ONLY: rows are never updated or hard-deleted (financial-evidence rule).
-- Each row carries a SHA-256 content hash so integrity can be independently verified later, and can
-- reference any entity — including a reversed transaction — so the proof survives the reversal.
CREATE TABLE evidence_items (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    entity_type  text        NOT NULL,          -- e.g. INVOICE, JOURNAL_ENTRY, PAYOUT, PAYMENT
    entity_id    char(26)    NOT NULL,
    content_hash char(64)    NOT NULL,          -- SHA-256 hex of the evidence content
    storage_ref  text,                          -- pointer to the stored artifact (object store key)
    description  text,
    source       text        NOT NULL,          -- UPLOAD | SYSTEM_EVENT
    uploaded_by  char(26),
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_evidence_entity ON evidence_items (tenant_id, entity_type, entity_id);
CREATE INDEX ix_evidence_hash ON evidence_items (tenant_id, content_hash);

-- Guard the immutability rule at the database layer: block UPDATE and DELETE on evidence rows.
CREATE OR REPLACE FUNCTION evidence_items_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'evidence_items is append-only; % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_evidence_items_no_update
    BEFORE UPDATE OR DELETE ON evidence_items
    FOR EACH ROW EXECUTE FUNCTION evidence_items_immutable();

-- Auditor export jobs: a request to bundle evidence/reports for an external auditor.
CREATE TABLE export_jobs (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    scope        text        NOT NULL,          -- describes what is being exported
    status       text        NOT NULL DEFAULT 'REQUESTED', -- REQUESTED|PROCESSING|COMPLETED|FAILED
    requested_by char(26)    NOT NULL,
    result_ref   text,
    error        text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    version      bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_export_jobs_tenant ON export_jobs (tenant_id, created_at DESC);

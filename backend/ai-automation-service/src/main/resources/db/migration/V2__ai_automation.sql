-- Sprint 15 AI automation with governance-by-design.
--
-- Every AI output carries a confidence and a governance decision. Low-confidence outputs are never
-- auto-applied; statutory/filing suggestions are NEVER auto-applied regardless of confidence; and a
-- human decision (accept/reject) is captured as feedback. All rows are tenant-scoped.

-- AI suggestions (e.g. a transaction category, an OCR-extracted bank detail, a cashflow forecast).
CREATE TABLE ai_suggestions (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    kind          text        NOT NULL,          -- TRANSACTION_CATEGORY | BANK_DETAIL_EXTRACTION | ...
    subject_type  text        NOT NULL,          -- entity type the suggestion is about
    subject_id    char(26),                       -- entity id (nullable for free-form)
    suggestion    jsonb       NOT NULL,          -- the proposed value(s)
    confidence    numeric(5,4) NOT NULL,          -- 0.0000 .. 1.0000
    decision      text        NOT NULL,          -- AUTO_APPLY | NEEDS_REVIEW | REJECT
    status        text        NOT NULL DEFAULT 'PROPOSED', -- PROPOSED|ACCEPTED|REJECTED|AUTO_APPLIED
    model_ref     text,                           -- which model/heuristic produced this
    reviewed_by   char(26),
    created_at    timestamptz NOT NULL DEFAULT now(),
    decided_at    timestamptz,
    version       bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_ai_suggestions_tenant ON ai_suggestions (tenant_id, status, created_at DESC);
CREATE INDEX ix_ai_suggestions_subject ON ai_suggestions (tenant_id, subject_type, subject_id);

-- Anomaly alerts raised by the detector (e.g. a payment far outside the historical range).
CREATE TABLE anomaly_alerts (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    subject_type  text        NOT NULL,
    subject_id    char(26),
    metric        text        NOT NULL,          -- what was measured (e.g. AMOUNT)
    observed_minor bigint     NOT NULL,
    score         numeric(8,4) NOT NULL,          -- anomaly score (e.g. robust z-score)
    severity      text        NOT NULL,          -- LOW | MEDIUM | HIGH
    status        text        NOT NULL DEFAULT 'OPEN', -- OPEN | ACKNOWLEDGED | DISMISSED
    detail        text,
    acknowledged_by char(26),
    created_at    timestamptz NOT NULL DEFAULT now(),
    version       bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_anomaly_alerts_tenant ON anomaly_alerts (tenant_id, status, created_at DESC);

-- Human feedback on AI outputs — the loop that lets the models improve and keeps decisions auditable.
CREATE TABLE ai_feedback (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    suggestion_id char(26)    NOT NULL,
    helpful       boolean     NOT NULL,
    note          text,
    given_by      char(26)    NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_ai_feedback_suggestion ON ai_feedback (tenant_id, suggestion_id);

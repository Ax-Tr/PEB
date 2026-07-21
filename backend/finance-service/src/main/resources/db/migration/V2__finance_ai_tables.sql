-- Complete AI automation tables in the consolidated finance database.
-- The original finance baseline had an older ai_suggestions shape and omitted later AI tables.

ALTER TABLE ai_suggestions
    ADD COLUMN IF NOT EXISTS kind text,
    ADD COLUMN IF NOT EXISTS subject_type text,
    ADD COLUMN IF NOT EXISTS subject_id char(26),
    ADD COLUMN IF NOT EXISTS decision text,
    ADD COLUMN IF NOT EXISTS model_ref text,
    ADD COLUMN IF NOT EXISTS reviewed_by char(26),
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

UPDATE ai_suggestions
SET
    kind = COALESCE(kind, suggestion_type),
    subject_type = COALESCE(subject_type, entity_type),
    subject_id = COALESCE(subject_id, entity_id),
    decision = COALESCE(decision, 'NEEDS_REVIEW')
WHERE kind IS NULL OR subject_type IS NULL OR decision IS NULL;

ALTER TABLE ai_suggestions
    ALTER COLUMN kind SET NOT NULL,
    ALTER COLUMN subject_type SET NOT NULL,
    ALTER COLUMN decision SET NOT NULL;

CREATE TABLE IF NOT EXISTS anomaly_alerts (
    id              char(26)      PRIMARY KEY,
    tenant_id       char(26)      NOT NULL,
    subject_type    text          NOT NULL,
    subject_id      char(26),
    metric          text          NOT NULL,
    observed_minor  bigint        NOT NULL,
    score           numeric(8,4)  NOT NULL,
    severity        text          NOT NULL,
    status          text          NOT NULL DEFAULT 'OPEN',
    detail          text,
    acknowledged_by char(26),
    created_at      timestamptz   NOT NULL DEFAULT now(),
    version         bigint        NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_anomaly_alerts_tenant
    ON anomaly_alerts (tenant_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_feedback (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    suggestion_id char(26)    NOT NULL,
    helpful       boolean     NOT NULL,
    note          text,
    given_by      char(26)    NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_ai_feedback_suggestion
    ON ai_feedback (tenant_id, suggestion_id);

CREATE TABLE IF NOT EXISTS voice_drafts (
    id                  char(26)      PRIMARY KEY,
    tenant_id           char(26)      NOT NULL,
    transcript_enc      text          NOT NULL,
    sanitized_enc       text          NOT NULL,
    intent              text          NOT NULL,
    status              text          NOT NULL,
    fields_enc          text          NOT NULL,
    missing_fields_enc  text          NOT NULL,
    confidence          numeric(5,4)  NOT NULL,
    suspicious          boolean       NOT NULL DEFAULT false,
    materialized_ref    char(26),
    rejection_reason    text,
    created_by          char(26),
    reviewed_by         char(26),
    created_at          timestamptz   NOT NULL,
    updated_at          timestamptz   NOT NULL,
    reviewed_at         timestamptz,
    CONSTRAINT ck_voice_drafts_status CHECK (status IN ('NEEDS_REVIEW','APPROVED','REJECTED')),
    CONSTRAINT ck_voice_drafts_confidence CHECK (confidence >= 0 AND confidence <= 1)
);
CREATE INDEX IF NOT EXISTS ix_voice_drafts_tenant_created
    ON voice_drafts (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_voice_drafts_tenant_status
    ON voice_drafts (tenant_id, status, created_at DESC);

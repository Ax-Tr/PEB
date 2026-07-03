-- Sprint 23: voice financial input drafts. Transcript/payload fields are encrypted in the entity.
CREATE TABLE voice_drafts (
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
CREATE INDEX ix_voice_drafts_tenant_created ON voice_drafts (tenant_id, created_at DESC);
CREATE INDEX ix_voice_drafts_tenant_status ON voice_drafts (tenant_id, status, created_at DESC);

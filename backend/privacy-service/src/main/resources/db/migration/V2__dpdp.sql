-- Sprint 16 DPDP (Digital Personal Data Protection Act) data-principal rights.
--
-- Data Subject Requests (DSR): access, correction, erasure, portability and grievance. The subject
-- email is PII → field-level encrypted. Erasure never hard-deletes financial/tax records (legal
-- retention); those are RETAINed and their PII ANONYMISED instead — captured in the erasure plan.
CREATE TABLE dsr_requests (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    type           text        NOT NULL,          -- ACCESS|CORRECTION|ERASURE|PORTABILITY|GRIEVANCE
    status         text        NOT NULL DEFAULT 'RECEIVED', -- RECEIVED|VERIFYING|IN_PROGRESS|COMPLETED|REJECTED
    subject_ref    char(26),                       -- internal id of the data principal, if known
    subject_email_enc text     NOT NULL,          -- AES-GCM ciphertext of the requester's email
    details        text,
    erasure_plan   jsonb,                          -- per-category outcome (DELETE|ANONYMIZE|RETAIN_LEGAL_HOLD)
    resolution_note text,
    evidence_ref   text,                           -- pointer to the fulfilment evidence bundle
    received_at    timestamptz NOT NULL DEFAULT now(),
    due_at         timestamptz NOT NULL,           -- statutory SLA
    verified_at    timestamptz,
    completed_at   timestamptz,
    handled_by     char(26),
    version        bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_dsr_tenant_status ON dsr_requests (tenant_id, status, received_at DESC);
CREATE INDEX ix_dsr_due ON dsr_requests (tenant_id, due_at) WHERE status NOT IN ('COMPLETED', 'REJECTED');

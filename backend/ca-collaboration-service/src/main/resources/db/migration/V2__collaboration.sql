-- Sprint 14 CA / accountant / auditor collaboration workspace.

-- Invitations for external collaborators. Email is PII → stored field-level encrypted.
-- role: ACCOUNTANT | CA | AUDITOR (AUDITOR is read-only). Access can be REVOKED mid-review.
CREATE TABLE ca_invites (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    email_enc      text        NOT NULL,          -- AES-GCM ciphertext of the invitee email
    role           text        NOT NULL,
    status         text        NOT NULL DEFAULT 'PENDING', -- PENDING|ACCEPTED|REVOKED|EXPIRED
    linked_user_id char(26),                       -- set to the accepting user's id
    invited_by     char(26)    NOT NULL,
    expires_at     timestamptz NOT NULL,
    accepted_at    timestamptz,
    revoked_at     timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_ca_invites_tenant ON ca_invites (tenant_id, status);
CREATE INDEX ix_ca_invites_user ON ca_invites (tenant_id, linked_user_id);

-- Append-only review comments on any entity (e.g. a compliance report, an invoice).
CREATE TABLE review_notes (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    entity_type text        NOT NULL,
    entity_id   char(26)    NOT NULL,
    author_id   char(26)    NOT NULL,
    note        text        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_review_notes_entity ON review_notes (tenant_id, entity_type, entity_id, created_at);

-- Report approval workflow (maker-checker): requester and approver must differ.
CREATE TABLE report_approvals (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    report_type   text        NOT NULL,
    report_ref    char(26)    NOT NULL,
    status        text        NOT NULL DEFAULT 'REQUESTED', -- REQUESTED|APPROVED|REJECTED
    requested_by  char(26)    NOT NULL,
    decided_by    char(26),
    decision_note text,
    requested_at  timestamptz NOT NULL DEFAULT now(),
    decided_at    timestamptz,
    version       bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_report_approvals_ref ON report_approvals (tenant_id, report_type, report_ref);

-- Month-end close checklist. A month cannot be locked until every mandatory item is complete.
CREATE TABLE close_checklists (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    period_year int         NOT NULL,
    period_month int        NOT NULL,
    created_by  char(26)    NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, period_year, period_month)
);

CREATE TABLE close_checklist_items (
    id           char(26)    PRIMARY KEY,
    checklist_id char(26)    NOT NULL REFERENCES close_checklists (id),
    tenant_id    char(26)    NOT NULL,
    label        text        NOT NULL,
    mandatory    boolean     NOT NULL DEFAULT true,
    done         boolean     NOT NULL DEFAULT false,
    done_by      char(26),
    done_at      timestamptz,
    sort_order   int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_checklist_items_checklist ON close_checklist_items (checklist_id, sort_order);

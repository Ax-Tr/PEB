-- Sprint 6 payout schema: verified beneficiaries, payouts with maker-checker approval.

CREATE TABLE beneficiaries (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    party_type          text        NOT NULL,               -- VENDOR, EMPLOYEE
    party_id            char(26)    NOT NULL,
    label               text,
    account_number_hash char(64)    NOT NULL,               -- HMAC blind index of the destination account
    status              text        NOT NULL DEFAULT 'ACTIVE',
    verified_at         timestamptz,                         -- when the destination was last verified/changed
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, party_type, party_id, account_number_hash)
);
CREATE INDEX ix_beneficiary_party ON beneficiaries (tenant_id, party_type, party_id);

CREATE TABLE payouts (
    id               char(26)    PRIMARY KEY,
    tenant_id        char(26)    NOT NULL,
    party_type       text        NOT NULL,
    party_id         char(26)    NOT NULL,
    beneficiary_id   char(26)    NOT NULL REFERENCES beneficiaries(id),
    amount_minor     bigint      NOT NULL CHECK (amount_minor > 0),
    purpose          text,
    status           text        NOT NULL,                  -- PENDING_APPROVAL, APPROVED, INITIATED, COMPLETED, FAILED, REJECTED
    risk_level       text        NOT NULL,                  -- LOW, HIGH
    provider         text,
    provider_ref     text,
    gateway_attempts int         NOT NULL DEFAULT 0,
    created_by       char(26),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    version          bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_payout_tenant_status ON payouts (tenant_id, status, created_at DESC);

-- Append-only maker-checker decision log.
CREATE TABLE payout_approvals (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    payout_id   char(26)    NOT NULL REFERENCES payouts(id),
    decision    text        NOT NULL,                       -- APPROVED, REJECTED
    approver_id char(26)    NOT NULL,
    reason      text,
    decided_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_payout_approvals_payout ON payout_approvals (payout_id);

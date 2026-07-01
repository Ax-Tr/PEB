-- Sprint 2: vendor profiles and their payout bank accounts.

CREATE TABLE vendors (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    mobile_enc  text,
    email_enc   text,
    gstin_enc   text,
    address     text,
    status      text        NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_vendor_tenant ON vendors (tenant_id, created_at DESC);

CREATE TABLE vendor_bank_accounts (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    vendor_id           char(26)    NOT NULL REFERENCES vendors (id),
    account_number_enc  text        NOT NULL,
    account_number_hash char(64)    NOT NULL,
    ifsc_enc            text        NOT NULL,
    upi_enc             text,
    bank_name           text        NOT NULL,
    holder_name         text        NOT NULL,
    status              text        NOT NULL DEFAULT 'PENDING_REVIEW',
    source              text        NOT NULL,
    reviewed_by         char(26),
    reviewed_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (vendor_id, account_number_hash)
);
CREATE INDEX ix_vendor_bank_accounts_vendor ON vendor_bank_accounts (vendor_id);

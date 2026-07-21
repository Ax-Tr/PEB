-- Sprint 1 tenant schema (merged into identity_db): business profile, branches, tax profile, settings.
-- businesses.id IS the canonical tenant_id used by every other service.

CREATE TABLE businesses (
    id             char(26)    PRIMARY KEY,
    owner_user_id  char(26)    NOT NULL,
    legal_name     text        NOT NULL,
    trade_name     text,
    business_type  text        NOT NULL,
    gstin_enc      text,
    gstin_hash     char(64),
    pan_enc        text,
    udyam          text,
    state_code     char(2)     NOT NULL,
    status         text        NOT NULL DEFAULT 'ACTIVE',
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_business_gstin ON businesses (gstin_hash) WHERE gstin_hash IS NOT NULL;
CREATE INDEX ix_business_owner ON businesses (owner_user_id);

CREATE TABLE branches (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL REFERENCES businesses(id),
    name        text        NOT NULL,
    state_code  char(2)     NOT NULL,
    address     text,
    gstin_enc   text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE TABLE business_tax_profiles (
    tenant_id             char(26)    PRIMARY KEY REFERENCES businesses(id),
    gst_registered        boolean     NOT NULL DEFAULT false,
    composition_scheme    boolean     NOT NULL DEFAULT false,
    reverse_charge_enabled boolean    NOT NULL DEFAULT false,
    default_place_of_supply char(2),
    tds_applicable        boolean     NOT NULL DEFAULT false,
    updated_at            timestamptz NOT NULL DEFAULT now(),
    version               bigint      NOT NULL DEFAULT 0
);

CREATE TABLE business_settings (
    tenant_id               char(26)    PRIMARY KEY REFERENCES businesses(id),
    invoice_prefix          text        NOT NULL DEFAULT 'INV',
    invoice_next_number     bigint      NOT NULL DEFAULT 1,
    upi_id_enc              text,
    logo_url                text,
    currency                char(3)     NOT NULL DEFAULT 'INR',
    financial_year_start_month int      NOT NULL DEFAULT 4,
    updated_at              timestamptz NOT NULL DEFAULT now(),
    version                 bigint      NOT NULL DEFAULT 0
);

-- DPDP privacy tables (merged from privacy-service)
CREATE TABLE dsr_requests (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    type              text        NOT NULL,
    status            text        NOT NULL,
    subject_ref       char(26),
    subject_email_enc text        NOT NULL,
    details           text,
    erasure_plan      jsonb,
    resolution_note   text,
    evidence_ref      text,
    received_at       timestamptz NOT NULL,
    due_at            timestamptz NOT NULL,
    verified_at       timestamptz,
    completed_at      timestamptz,
    handled_by        char(26),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_dsr_requests_tenant ON dsr_requests (tenant_id, status);

-- Sprint 1 tenant schema: business profile, branches, tax profile, settings.
-- businesses.id IS the canonical tenant_id used by every other service.

CREATE TABLE businesses (
    id             char(26)    PRIMARY KEY,
    owner_user_id  char(26)    NOT NULL,
    legal_name     text        NOT NULL,
    trade_name     text,
    business_type  text        NOT NULL,               -- PROPRIETOR, PARTNERSHIP, LLP, PVT_LTD, ...
    gstin_enc      text,                                -- AES-GCM ciphertext (nullable: not all MSMEs are GST-registered)
    gstin_hash     char(64),                            -- HMAC blind index for uniqueness/search
    pan_enc        text,
    udyam          text,
    state_code     char(2)     NOT NULL,                -- GST state code, place of supply default
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
    financial_year_start_month int      NOT NULL DEFAULT 4,   -- Indian FY starts April
    updated_at              timestamptz NOT NULL DEFAULT now(),
    version                 bigint      NOT NULL DEFAULT 0
);

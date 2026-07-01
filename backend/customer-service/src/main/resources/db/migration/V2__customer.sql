-- Sprint 2 customer schema: customer directory and contact channels.
-- Tenant-scoped by tenant_id (the businesses.id owned by tenant-service).

CREATE TABLE customers (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    mobile_enc  text        NOT NULL,               -- AES-GCM ciphertext
    mobile_hash char(64)    NOT NULL,               -- HMAC blind index for uniqueness/search
    email_enc   text,
    address     text,
    gstin_enc   text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_customer_tenant_mobile ON customers (tenant_id, mobile_hash);
CREATE INDEX ix_customer_tenant ON customers (tenant_id, created_at DESC);

CREATE TABLE customer_contacts (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    customer_id char(26)    NOT NULL REFERENCES customers(id),
    type        text        NOT NULL,               -- PHONE, EMAIL, WHATSAPP
    value_enc   text        NOT NULL,               -- AES-GCM ciphertext
    preferred   boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now()
);

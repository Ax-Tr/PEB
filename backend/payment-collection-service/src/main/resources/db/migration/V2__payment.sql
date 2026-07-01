-- Sprint 3 payment-collection schema: payment requests, dynamic QR/links, webhook ledger.

CREATE TABLE payment_requests (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    customer_id       char(26),
    reference         text        NOT NULL,                 -- UPI txn ref (tr), unique per tenant
    amount_minor      bigint      NOT NULL CHECK (amount_minor > 0),
    amount_paid_minor bigint      NOT NULL DEFAULT 0 CHECK (amount_paid_minor >= 0),
    allow_partial     boolean     NOT NULL DEFAULT false,
    purpose           text,
    status            text        NOT NULL DEFAULT 'AWAITING_PAYMENT',
    invoice_id        char(26),                              -- set by the invoice/saga flow later
    provider          text,
    expires_at        timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_payment_reference ON payment_requests (tenant_id, reference);
CREATE INDEX ix_payment_tenant_status ON payment_requests (tenant_id, status, created_at DESC);

CREATE TABLE payment_qr_codes (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    payment_request_id char(26)    NOT NULL REFERENCES payment_requests(id),
    upi_uri            text        NOT NULL,                 -- upi://pay?... (QR content)
    payment_link       text        NOT NULL,
    created_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_qr_request ON payment_qr_codes (payment_request_id);

-- Append-only inbound webhook ledger. Idempotent on (provider, provider_event_id): a redelivered
-- webhook is a no-op. raw_payload retained as evidence.
CREATE TABLE payment_webhooks (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26),
    provider          text        NOT NULL,
    provider_event_id text        NOT NULL,
    signature_verified boolean    NOT NULL,
    status            text        NOT NULL,                  -- RECEIVED, PROCESSED, REJECTED
    reference         text,
    raw_payload       jsonb       NOT NULL,
    received_at       timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_event_id)
);

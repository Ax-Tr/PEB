-- Bring the consolidated finance payment-collection schema in line with the current module model.

ALTER TABLE payment_requests
    ADD COLUMN IF NOT EXISTS reference text,
    ADD COLUMN IF NOT EXISTS amount_paid_minor bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS allow_partial boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS purpose text,
    ADD COLUMN IF NOT EXISTS provider text;

UPDATE payment_requests
SET
    reference = COALESCE(reference, gateway_order_id, id),
    provider = COALESCE(provider, gateway),
    amount_paid_minor = COALESCE(amount_paid_minor, 0),
    status = CASE status
        WHEN 'CREATED' THEN 'AWAITING_PAYMENT'
        WHEN 'SUCCESS' THEN 'PAID'
        ELSE status
    END
WHERE reference IS NULL
   OR provider IS NULL
   OR amount_paid_minor IS NULL
   OR status IN ('CREATED', 'SUCCESS');

ALTER TABLE payment_requests
    ALTER COLUMN reference SET NOT NULL;

CREATE TABLE IF NOT EXISTS payment_qr_codes (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    payment_request_id char(26)    NOT NULL,
    upi_uri            text        NOT NULL,
    payment_link       text        NOT NULL,
    created_at         timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_payment_qr_codes_request
    ON payment_qr_codes (payment_request_id);

CREATE TABLE IF NOT EXISTS payment_webhooks (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26),
    provider           text        NOT NULL,
    provider_event_id  text        NOT NULL,
    signature_verified boolean     NOT NULL,
    status             text        NOT NULL,
    reference          text,
    raw_payload        jsonb       NOT NULL,
    received_at        timestamptz NOT NULL,
    UNIQUE (provider, provider_event_id)
);

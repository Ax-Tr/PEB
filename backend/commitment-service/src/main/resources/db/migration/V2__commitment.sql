-- Sprint 20 commitment schema: promises, due dates, partial payment tracking, broken promises.

CREATE TABLE commitments (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    counterparty_type   text        NOT NULL, -- CUSTOMER, VENDOR, EMPLOYEE, OTHER
    counterparty_id     char(26),
    counterparty_name   text,
    source_type         text        NOT NULL, -- MANUAL, INVOICE, PAYMENT_REQUEST, INSTALLMENT, VOICE, OCR_NOTE
    source_ref          char(26),
    description         text,
    amount_minor        bigint      NOT NULL CHECK (amount_minor > 0),
    paid_minor          bigint      NOT NULL DEFAULT 0 CHECK (paid_minor >= 0),
    due_date            date        NOT NULL,
    status              text        NOT NULL DEFAULT 'PROMISED',
    created_by          char(26),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    closed_at           timestamptz,
    version             bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_commitment_paid_lte_amount CHECK (paid_minor <= amount_minor)
);
CREATE INDEX ix_commitment_tenant_status_due ON commitments (tenant_id, status, due_date);
CREATE INDEX ix_commitment_tenant_counterparty ON commitments (tenant_id, counterparty_type, counterparty_id);
CREATE UNIQUE INDEX ux_commitment_source ON commitments (tenant_id, source_type, source_ref)
    WHERE source_ref IS NOT NULL;

CREATE TABLE commitment_events (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    commitment_id     char(26)    NOT NULL REFERENCES commitments(id),
    event_type        text        NOT NULL,
    old_due_date      date,
    new_due_date      date,
    amount_minor      bigint,
    note              text,
    actor_id          char(26),
    occurred_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_commitment_events_commitment_time ON commitment_events (commitment_id, occurred_at DESC);

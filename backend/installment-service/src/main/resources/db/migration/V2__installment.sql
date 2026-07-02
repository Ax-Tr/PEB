-- Sprint 8 installment schema: receivable/payable EMI schedules + per-EMI tracking.

CREATE TABLE installments (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    type              text        NOT NULL,                 -- RECEIVABLE, PAYABLE
    counterparty_id   char(26),                             -- customer (receivable) or vendor (payable)
    counterparty_name text,
    source_type       text,                                 -- INVOICE, PURCHASE_BILL, MANUAL
    source_ref        char(26),                             -- originating invoice/bill id
    total_amount_minor bigint     NOT NULL CHECK (total_amount_minor > 0),
    outstanding_minor  bigint     NOT NULL,
    number_of_emis    int         NOT NULL,
    frequency         text        NOT NULL DEFAULT 'MONTHLY',
    status            text        NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, CLOSED, CANCELLED
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_installment_tenant_type ON installments (tenant_id, type, status);
-- One schedule per source document (when linked): dedupe accidental double-creation.
CREATE UNIQUE INDEX ux_installment_source ON installments (tenant_id, source_ref)
    WHERE source_ref IS NOT NULL;

CREATE TABLE installment_emis (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    installment_id char(26)    NOT NULL REFERENCES installments(id),
    emi_number     int         NOT NULL,
    due_date       date        NOT NULL,
    amount_minor   bigint      NOT NULL,
    paid_minor     bigint      NOT NULL DEFAULT 0,
    status         text        NOT NULL DEFAULT 'PENDING',  -- PENDING, PARTIAL, PAID
    paid_at        timestamptz,
    UNIQUE (installment_id, emi_number)
);
CREATE INDEX ix_emi_due ON installment_emis (tenant_id, due_date) WHERE status <> 'PAID';

-- Sprint 11 reconciliation schema. Reconcilable items come from two sides: EXTERNAL (imported bank/
-- UPI/settlement rows — the source of truth) and INTERNAL (payments/invoices/payouts). The weighted
-- match engine pairs them; unmatched items become exceptions requiring review.

CREATE TABLE recon_items (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    side         text        NOT NULL,                 -- EXTERNAL, INTERNAL
    source_type  text        NOT NULL,                 -- BANK_TXN, PAYMENT, PAYOUT, INVOICE, SALARY_RUN
    source_ref   char(26)    NOT NULL,                 -- originating entity id
    direction    text        NOT NULL,                 -- CREDIT, DEBIT
    amount_minor bigint      NOT NULL,
    item_date    date        NOT NULL,
    reference    text,                                  -- UTR/UPI id/gateway id/invoice number
    counterparty text,
    narration    text,
    matched      boolean     NOT NULL DEFAULT false,
    match_id     char(26),
    created_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, side, source_type, source_ref)
);
CREATE INDEX ix_recon_unmatched
    ON recon_items (tenant_id, side, direction, matched) WHERE matched = false;
CREATE INDEX ix_recon_amount ON recon_items (tenant_id, amount_minor) WHERE matched = false;

CREATE TABLE reconciliation_matches (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    external_item_id  char(26)    NOT NULL REFERENCES recon_items(id),
    internal_item_id  char(26)    NOT NULL REFERENCES recon_items(id),
    score             numeric(4,3) NOT NULL,
    status            text        NOT NULL,             -- AUTO, SUGGESTED, CONFIRMED, REJECTED
    matched_by        char(26),                          -- actor for manual/confirm
    created_at        timestamptz NOT NULL DEFAULT now(),
    decided_at        timestamptz
);
CREATE INDEX ix_match_tenant_status ON reconciliation_matches (tenant_id, status, created_at DESC);

CREATE TABLE reconciliation_exceptions (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    item_id      char(26)    NOT NULL REFERENCES recon_items(id),
    reason       text        NOT NULL,
    status       text        NOT NULL DEFAULT 'OPEN',  -- OPEN, RESOLVED
    created_at   timestamptz NOT NULL DEFAULT now(),
    resolved_at  timestamptz,
    UNIQUE (tenant_id, item_id)
);
CREATE INDEX ix_exception_open ON reconciliation_exceptions (tenant_id, status) WHERE status = 'OPEN';

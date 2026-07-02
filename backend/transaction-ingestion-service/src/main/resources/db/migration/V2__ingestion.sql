-- Sprint 10 transaction-monitor schema. Cash/bank/UPI/settlement rows share one `transactions`
-- table keyed by `source`; imported rows are deduped by `dedupe_hash` (idempotent import) and are
-- the external source of truth for reconciliation (Sprint 11) — they are NOT posted to the ledger.

CREATE TABLE bank_accounts (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    bank_name           text        NOT NULL,
    account_number_enc  text        NOT NULL,               -- AES-GCM ciphertext
    account_number_hash char(64)    NOT NULL,               -- HMAC blind index
    ifsc_enc            text,
    account_type        text,                                -- SAVINGS, CURRENT
    opening_balance_minor bigint    NOT NULL DEFAULT 0,
    currency            char(3)     NOT NULL DEFAULT 'INR',
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, account_number_hash)
);

CREATE TABLE import_batches (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    bank_account_id char(26),
    source          text        NOT NULL,                   -- BANK_IMPORT, UPI_IMPORT, GATEWAY_SETTLEMENT
    file_name       text,
    total_rows      int         NOT NULL DEFAULT 0,
    imported_count  int         NOT NULL DEFAULT 0,
    duplicate_count int         NOT NULL DEFAULT 0,
    status          text        NOT NULL DEFAULT 'COMPLETED',
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id                      char(26)    PRIMARY KEY,
    tenant_id               char(26)    NOT NULL,
    bank_account_id         char(26),                        -- null for cash
    source                  text        NOT NULL,            -- MANUAL_CASH, MANUAL_BANK, BANK_IMPORT, UPI_IMPORT, GATEWAY_SETTLEMENT
    direction               text        NOT NULL,            -- CREDIT, DEBIT
    amount_minor            bigint      NOT NULL CHECK (amount_minor > 0),
    txn_date                date        NOT NULL,
    narration               text,
    external_ref            text,                            -- UTR / UPI txn id / settlement id
    counterparty            text,
    dedupe_hash             char(64)    NOT NULL,
    category                text,
    classification_status   text        NOT NULL DEFAULT 'UNCLASSIFIED', -- UNCLASSIFIED, SUGGESTED, CONFIRMED
    classification_confidence numeric(4,3) NOT NULL DEFAULT 0,
    reconciled              boolean     NOT NULL DEFAULT false,
    import_batch_id         char(26),
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);
-- Idempotent import / duplicate detection: a transaction's natural key is unique per tenant.
CREATE UNIQUE INDEX ux_txn_dedupe ON transactions (tenant_id, dedupe_hash);
CREATE INDEX ix_txn_tenant_date ON transactions (tenant_id, txn_date DESC);
CREATE INDEX ix_txn_review ON transactions (tenant_id, classification_status)
    WHERE classification_status = 'SUGGESTED';
CREATE INDEX ix_txn_unreconciled ON transactions (tenant_id, reconciled) WHERE reconciled = false;

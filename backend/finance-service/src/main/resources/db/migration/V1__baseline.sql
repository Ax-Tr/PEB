-- Consolidated finance-service baseline schema.
-- Merges: payment-collection, payout, accounting-ledger, installment,
--         transaction-ingestion, reconciliation, compliance-report, analytics,
--         audit-evidence, ai-automation.

-- ===== OUTBOX =====
CREATE TABLE outbox_events (
    id              char(26)    PRIMARY KEY,
    aggregate_type  text        NOT NULL,
    aggregate_id    char(26)    NOT NULL,
    event_type      text        NOT NULL,
    event_version   int         NOT NULL DEFAULT 1,
    tenant_id       char(26)    NOT NULL,
    payload         jsonb       NOT NULL,
    headers         jsonb       NOT NULL DEFAULT '{}',
    created_at      timestamptz NOT NULL DEFAULT now(),
    published_at    timestamptz,
    attempts        int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- ===== IDEMPOTENCY =====
CREATE TABLE idempotency_keys (
    tenant_id    char(26)    NOT NULL,
    key          text        NOT NULL,
    endpoint     text        NOT NULL,
    request_hash text        NOT NULL,
    status       text        NOT NULL,
    response     jsonb,
    created_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, key)
);

-- ===== AUDIT =====
CREATE TABLE audit_events (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    actor_id       char(26),
    event_type     text        NOT NULL,
    entity_type    text,
    entity_id      char(26),
    correlation_id text,
    occurred_at    timestamptz NOT NULL DEFAULT now(),
    data           jsonb       NOT NULL DEFAULT '{}'
);
CREATE INDEX ix_audit_tenant_time ON audit_events (tenant_id, occurred_at DESC);

-- ===== PAYMENT-COLLECTION MODULE =====
CREATE TABLE payment_requests (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    customer_id       char(26),
    invoice_id        char(26),
    amount_minor      bigint      NOT NULL CHECK (amount_minor > 0),
    currency          char(3)     NOT NULL DEFAULT 'INR',
    status            text        NOT NULL DEFAULT 'CREATED',
    gateway           text,
    gateway_order_id  text,
    gateway_payment_id text,
    payment_method    text,
    paid_at           timestamptz,
    expires_at        timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_payment_req_tenant ON payment_requests (tenant_id, created_at DESC);
CREATE INDEX ix_payment_req_invoice ON payment_requests (tenant_id, invoice_id) WHERE invoice_id IS NOT NULL;

CREATE TABLE payment_webhook_events (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    gateway         text        NOT NULL,
    event_type      text        NOT NULL,
    payload         jsonb       NOT NULL,
    idempotency_key text        NOT NULL,
    processed       boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (gateway, idempotency_key)
);

-- ===== PAYOUT MODULE =====
CREATE TABLE beneficiaries (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    name                text        NOT NULL,
    account_number_enc  text        NOT NULL,
    account_number_hash char(64)    NOT NULL,
    ifsc_enc            text        NOT NULL,
    upi_enc             text,
    status              text        NOT NULL DEFAULT 'ACTIVE',
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, account_number_hash)
);

CREATE TABLE payouts (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    beneficiary_id    char(26)    NOT NULL REFERENCES beneficiaries(id),
    amount_minor      bigint      NOT NULL CHECK (amount_minor > 0),
    currency          char(3)     NOT NULL DEFAULT 'INR',
    purpose           text,
    status            text        NOT NULL DEFAULT 'INITIATED',
    gateway           text,
    gateway_ref       text,
    utr               text,
    failure_reason    text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_payout_tenant ON payouts (tenant_id, created_at DESC);

-- ===== ACCOUNTING-LEDGER MODULE =====
CREATE TABLE chart_of_accounts (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    code        text        NOT NULL,
    name        text        NOT NULL,
    type        text        NOT NULL,
    parent_id   char(26),
    system_account boolean  NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);

CREATE TABLE accounting_periods (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    year        int         NOT NULL,
    month       int         NOT NULL,
    status      text        NOT NULL DEFAULT 'OPEN',
    closed_at   timestamptz,
    closed_by   char(26),
    UNIQUE (tenant_id, year, month)
);

CREATE TABLE journal_entries (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    period_id       char(26)    NOT NULL REFERENCES accounting_periods(id),
    entry_date      date        NOT NULL,
    narration       text        NOT NULL,
    source          text,
    source_ref      char(26),
    status          text        NOT NULL DEFAULT 'POSTED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    created_by      char(26),
    version         bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_journal_tenant ON journal_entries (tenant_id, entry_date DESC);

CREATE TABLE journal_lines (
    id              char(26)    PRIMARY KEY,
    journal_id      char(26)    NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    tenant_id       char(26)    NOT NULL,
    account_id      char(26)    NOT NULL REFERENCES chart_of_accounts(id),
    debit_minor     bigint      NOT NULL DEFAULT 0,
    credit_minor    bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_journal_line_side CHECK (
        (debit_minor > 0 AND credit_minor = 0) OR (debit_minor = 0 AND credit_minor > 0)
    )
);
CREATE INDEX ix_journal_lines_journal ON journal_lines (journal_id);
CREATE INDEX ix_journal_lines_account ON journal_lines (tenant_id, account_id);

-- ===== INSTALLMENT MODULE =====
CREATE TABLE installment_plans (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    customer_id       char(26),
    invoice_id        char(26),
    total_minor       bigint      NOT NULL,
    num_installments  int         NOT NULL,
    frequency         text        NOT NULL DEFAULT 'MONTHLY',
    start_date        date        NOT NULL,
    status            text        NOT NULL DEFAULT 'ACTIVE',
    created_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_installment_plan_tenant ON installment_plans (tenant_id, status);

CREATE TABLE installments (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    plan_id         char(26)    NOT NULL REFERENCES installment_plans(id),
    emi_number      int         NOT NULL,
    amount_minor    bigint      NOT NULL,
    due_date        date        NOT NULL,
    status          text        NOT NULL DEFAULT 'PENDING',
    paid_at         timestamptz,
    payment_ref     char(26),
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (plan_id, emi_number)
);
CREATE INDEX ix_installment_due ON installments (tenant_id, due_date) WHERE status = 'PENDING';

-- ===== TRANSACTION-INGESTION MODULE =====
CREATE TABLE bank_accounts (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    bank_name           text        NOT NULL,
    account_number_enc  text        NOT NULL,
    account_number_hash char(64)    NOT NULL,
    ifsc_enc            text,
    account_type        text,
    opening_balance_minor bigint    NOT NULL DEFAULT 0,
    currency            char(3)     NOT NULL DEFAULT 'INR',
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, account_number_hash)
);

CREATE TABLE import_batches (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    bank_account_id char(26),
    source          text        NOT NULL,
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
    bank_account_id         char(26),
    source                  text        NOT NULL,
    direction               text        NOT NULL,
    amount_minor            bigint      NOT NULL CHECK (amount_minor > 0),
    txn_date                date        NOT NULL,
    narration               text,
    external_ref            text,
    counterparty            text,
    dedupe_hash             char(64)    NOT NULL,
    category                text,
    classification_status   text        NOT NULL DEFAULT 'UNCLASSIFIED',
    classification_confidence numeric(4,3) NOT NULL DEFAULT 0,
    reconciled              boolean     NOT NULL DEFAULT false,
    import_batch_id         char(26),
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_txn_dedupe ON transactions (tenant_id, dedupe_hash);
CREATE INDEX ix_txn_tenant_date ON transactions (tenant_id, txn_date DESC);

-- ===== RECONCILIATION MODULE =====
CREATE TABLE recon_rules (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    match_type  text        NOT NULL,
    config      jsonb       NOT NULL DEFAULT '{}',
    priority    int         NOT NULL DEFAULT 0,
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE TABLE recon_runs (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    status          text        NOT NULL DEFAULT 'RUNNING',
    total_items     int         NOT NULL DEFAULT 0,
    matched_count   int         NOT NULL DEFAULT 0,
    exception_count int         NOT NULL DEFAULT 0,
    started_at      timestamptz NOT NULL DEFAULT now(),
    completed_at    timestamptz
);

CREATE TABLE recon_items (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    run_id          char(26)    NOT NULL REFERENCES recon_runs(id),
    transaction_id  char(26),
    source_type     text        NOT NULL,
    source_ref      text,
    amount_minor    bigint      NOT NULL,
    txn_date        date        NOT NULL,
    status          text        NOT NULL DEFAULT 'UNMATCHED',
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_recon_items_run ON recon_items (run_id, status);

CREATE TABLE reconciliation_matches (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    item_a_id       char(26)    NOT NULL REFERENCES recon_items(id),
    item_b_id       char(26)    NOT NULL REFERENCES recon_items(id),
    rule_id         char(26)    REFERENCES recon_rules(id),
    confidence      numeric(4,3) NOT NULL DEFAULT 1.0,
    status          text        NOT NULL DEFAULT 'SUGGESTED',
    created_at      timestamptz NOT NULL DEFAULT now(),
    decided_at      timestamptz
);

CREATE TABLE reconciliation_exceptions (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    item_id      char(26)    NOT NULL REFERENCES recon_items(id),
    reason       text        NOT NULL,
    status       text        NOT NULL DEFAULT 'OPEN',
    created_at   timestamptz NOT NULL DEFAULT now(),
    resolved_at  timestamptz,
    UNIQUE (tenant_id, item_id)
);

-- ===== COMPLIANCE-REPORT MODULE =====
CREATE TABLE compliance_reports (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    report_type     text        NOT NULL,
    period_year     int         NOT NULL,
    period_month    int,
    quarter         int,
    status          text        NOT NULL DEFAULT 'DRAFT',
    data            jsonb       NOT NULL DEFAULT '{}',
    generated_at    timestamptz NOT NULL DEFAULT now(),
    filed_at        timestamptz,
    version         bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_compliance_tenant ON compliance_reports (tenant_id, report_type, period_year);

-- ===== ANALYTICS MODULE =====
CREATE TABLE analytics_snapshots (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    snapshot_type   text        NOT NULL,
    period_key      text        NOT NULL,
    data            jsonb       NOT NULL DEFAULT '{}',
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, snapshot_type, period_key)
);

-- ===== AUDIT-EVIDENCE MODULE =====
CREATE TABLE audit_evidence (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    category        text        NOT NULL,
    entity_type     text        NOT NULL,
    entity_id       char(26)    NOT NULL,
    evidence_type   text        NOT NULL,
    description     text,
    document_id     char(26),
    created_at      timestamptz NOT NULL DEFAULT now(),
    created_by      char(26)
);
CREATE INDEX ix_audit_evidence_tenant ON audit_evidence (tenant_id, category, created_at DESC);

-- ===== AI-AUTOMATION MODULE =====
CREATE TABLE ai_rules (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    rule_type       text        NOT NULL,
    name            text        NOT NULL,
    conditions      jsonb       NOT NULL DEFAULT '{}',
    actions         jsonb       NOT NULL DEFAULT '{}',
    active          boolean     NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, rule_type, name)
);

CREATE TABLE ai_suggestions (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    suggestion_type text        NOT NULL,
    entity_type     text        NOT NULL,
    entity_id       char(26)    NOT NULL,
    suggestion      jsonb       NOT NULL,
    confidence      numeric(4,3) NOT NULL DEFAULT 0,
    status          text        NOT NULL DEFAULT 'PENDING',
    created_at      timestamptz NOT NULL DEFAULT now(),
    decided_at      timestamptz
);
CREATE INDEX ix_ai_suggestions_tenant ON ai_suggestions (tenant_id, status, created_at DESC);

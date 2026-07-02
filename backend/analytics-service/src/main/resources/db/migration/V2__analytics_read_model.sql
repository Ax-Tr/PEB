-- Sprint 13 analytics read-model. These tables are DERIVED, event-fed projections — analytics
-- never queries the OLTP services' databases. They are denormalised and query-shaped so the store
-- can be swapped for ClickHouse in production without changing the compute/query layer.
--
-- Money is stored in integer paise (_minor). All period columns are derived in Asia/Kolkata (IST),
-- because that is the statutory/reporting timezone for Indian MSMEs.

-- Revenue + receivables projection (one row per invoice).
CREATE TABLE fact_invoices (
    tenant_id         char(26)    NOT NULL,
    invoice_id        char(26)    NOT NULL,
    invoice_number    text,
    customer_id       char(26),
    branch_id         char(26),
    invoice_date      date        NOT NULL,
    period_year       int         NOT NULL,
    period_month      int         NOT NULL,
    supply_type       text,
    taxable_minor     bigint      NOT NULL DEFAULT 0,
    tax_minor         bigint      NOT NULL DEFAULT 0,
    total_minor       bigint      NOT NULL DEFAULT 0,
    amount_paid_minor bigint      NOT NULL DEFAULT 0,
    status            text        NOT NULL DEFAULT 'ISSUED',
    updated_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (invoice_id)
);
CREATE INDEX ix_fact_invoices_period ON fact_invoices (tenant_id, period_year, period_month);
CREATE INDEX ix_fact_invoices_customer ON fact_invoices (tenant_id, customer_id);

-- Cost + payables projection (one row per purchase bill).
CREATE TABLE fact_purchases (
    tenant_id         char(26)    NOT NULL,
    bill_id           char(26)    NOT NULL,
    vendor_id         char(26),
    branch_id         char(26),
    bill_date         date        NOT NULL,
    period_year       int         NOT NULL,
    period_month      int         NOT NULL,
    net_minor         bigint      NOT NULL DEFAULT 0,
    input_gst_minor   bigint      NOT NULL DEFAULT 0,
    total_minor       bigint      NOT NULL DEFAULT 0,
    amount_paid_minor bigint      NOT NULL DEFAULT 0,
    status            text        NOT NULL DEFAULT 'RECORDED',
    updated_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (bill_id)
);
CREATE INDEX ix_fact_purchases_period ON fact_purchases (tenant_id, period_year, period_month);
CREATE INDEX ix_fact_purchases_vendor ON fact_purchases (tenant_id, vendor_id);

-- Cashflow projection (one row per confirmed cash movement: collection inflow or payout outflow).
CREATE TABLE fact_cash_movements (
    tenant_id      char(26)    NOT NULL,
    movement_id    char(26)    NOT NULL,
    direction      text        NOT NULL,               -- INFLOW | OUTFLOW
    source         text        NOT NULL,               -- PAYMENT | PAYOUT
    counterparty_id char(26),
    branch_id      char(26),
    occurred_on    date        NOT NULL,
    period_year    int         NOT NULL,
    period_month   int         NOT NULL,
    amount_minor   bigint      NOT NULL DEFAULT 0,
    created_at     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (movement_id)
);
CREATE INDEX ix_fact_cash_period ON fact_cash_movements (tenant_id, period_year, period_month);

-- Operating expense projection (one row per approved expense), fed from EXPENSE_APPROVED.
CREATE TABLE fact_expenses (
    tenant_id     char(26)    NOT NULL,
    expense_id    char(26)    NOT NULL,
    category      text,
    branch_id     char(26),
    occurred_on   date        NOT NULL,
    period_year   int         NOT NULL,
    period_month  int         NOT NULL,
    amount_minor  bigint      NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (expense_id)
);
CREATE INDEX ix_fact_expenses_period ON fact_expenses (tenant_id, period_year, period_month);

-- Product / service profitability projection (one row per invoice line, when line detail is
-- available on the source event; otherwise not populated — flagged as a follow-up).
CREATE TABLE fact_product_sales (
    tenant_id     char(26)    NOT NULL,
    line_id       char(26)    NOT NULL,
    invoice_id    char(26)    NOT NULL,
    product_id    char(26),
    product_name  text,
    period_year   int         NOT NULL,
    period_month  int         NOT NULL,
    quantity      numeric(18,3) NOT NULL DEFAULT 0,
    revenue_minor bigint      NOT NULL DEFAULT 0,
    cost_minor    bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (line_id)
);
CREATE INDEX ix_fact_product_period ON fact_product_sales (tenant_id, period_year, period_month, product_id);

-- Freshness / read-model lag indicator. One row per (tenant, source stream). A dashboard reads
-- this to show how current its data is; last_processed_at drives the staleness computation.
CREATE TABLE stream_watermarks (
    id                text        PRIMARY KEY,          -- surrogate: tenant_id + '|' + stream
    tenant_id         char(26)    NOT NULL,
    stream            text        NOT NULL,             -- e.g. invoice.events, payment.events
    last_event_id     char(26),
    last_processed_at timestamptz NOT NULL DEFAULT now(),
    events_processed  bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, stream)
);

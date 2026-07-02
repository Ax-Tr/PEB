-- Sprint 4 purchase & expense schema. Purchase bills capture input GST (ITC); expenses go through
-- a maker-checker approval. All amounts are integer paise.

CREATE TABLE purchase_bills (
    id                     char(26)    PRIMARY KEY,
    tenant_id              char(26)    NOT NULL,
    vendor_id              char(26),
    vendor_name            text,
    vendor_gstin           text,
    bill_number            text,
    bill_date              date        NOT NULL,
    place_of_supply        char(2)     NOT NULL,               -- GST state code
    business_state_code    char(2)     NOT NULL,
    reverse_charge         boolean     NOT NULL DEFAULT false,
    total_taxable_minor    bigint      NOT NULL DEFAULT 0,
    total_input_gst_minor  bigint      NOT NULL DEFAULT 0,
    total_cgst_minor       bigint      NOT NULL DEFAULT 0,
    total_sgst_minor       bigint      NOT NULL DEFAULT 0,
    total_igst_minor       bigint      NOT NULL DEFAULT 0,
    total_amount_minor     bigint      NOT NULL DEFAULT 0,
    status                 text        NOT NULL DEFAULT 'RECORDED',
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),
    version                bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_purchase_tenant_date ON purchase_bills (tenant_id, bill_date DESC);

CREATE TABLE purchase_items (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    purchase_bill_id    char(26)    NOT NULL REFERENCES purchase_bills(id),
    product_id          char(26),
    description         text        NOT NULL,
    hsn_sac             text,
    quantity            numeric(18,3) NOT NULL,
    unit_price_minor    bigint      NOT NULL,
    discount_minor      bigint      NOT NULL DEFAULT 0,
    gst_rate            numeric(5,2) NOT NULL DEFAULT 0,
    taxable_value_minor bigint      NOT NULL,
    cgst_minor          bigint      NOT NULL DEFAULT 0,
    sgst_minor          bigint      NOT NULL DEFAULT 0,
    igst_minor          bigint      NOT NULL DEFAULT 0,
    line_total_minor    bigint      NOT NULL
);
CREATE INDEX ix_purchase_items_bill ON purchase_items (purchase_bill_id);

CREATE TABLE expenses (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    category        text        NOT NULL,
    description     text,
    amount_minor    bigint      NOT NULL,
    gst_rate        numeric(5,2) NOT NULL DEFAULT 0,
    input_gst_minor bigint      NOT NULL DEFAULT 0,
    vendor_id       char(26),
    expense_date    date        NOT NULL,
    status          text        NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by     char(26),
    approved_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_expenses_tenant_date ON expenses (tenant_id, expense_date DESC);

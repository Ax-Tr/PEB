-- Invoice/GST tables for the consolidated business database.

CREATE TABLE IF NOT EXISTS document_sequences (
    tenant_id      char(26) NOT NULL,
    doc_type       text     NOT NULL,
    financial_year text     NOT NULL,
    prefix         text     NOT NULL,
    next_number    bigint   NOT NULL,
    PRIMARY KEY (tenant_id, doc_type, financial_year)
);

CREATE TABLE IF NOT EXISTS invoices (
    id                    char(26)    PRIMARY KEY,
    tenant_id             char(26)    NOT NULL,
    document_type         text        NOT NULL,
    supply_type           text        NOT NULL,
    customer_id           char(26),
    customer_name         text,
    customer_gstin        text,
    place_of_supply       char(2)     NOT NULL,
    business_state_code   char(2)     NOT NULL,
    invoice_number        text        NOT NULL,
    financial_year        text        NOT NULL,
    invoice_date          date        NOT NULL,
    original_document_id  char(26),
    reason                text,
    reverse_charge        boolean     NOT NULL,
    taxable               boolean     NOT NULL,
    total_taxable_minor   bigint      NOT NULL,
    total_cgst_minor      bigint      NOT NULL,
    total_sgst_minor      bigint      NOT NULL,
    total_igst_minor      bigint      NOT NULL,
    total_tax_minor       bigint      NOT NULL,
    total_amount_minor    bigint      NOT NULL,
    status                text        NOT NULL,
    payment_request_id    char(26),
    created_at            timestamptz NOT NULL,
    updated_at            timestamptz NOT NULL,
    version               bigint      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_invoices_tenant_date
    ON invoices (tenant_id, invoice_date DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_tenant_number
    ON invoices (tenant_id, invoice_number);

CREATE TABLE IF NOT EXISTS invoice_items (
    id                    char(26)      PRIMARY KEY,
    tenant_id             char(26)      NOT NULL,
    invoice_id            char(26)      NOT NULL,
    product_id            char(26),
    description           text          NOT NULL,
    hsn_sac               text,
    quantity              numeric(18,3) NOT NULL,
    unit_price_minor      bigint        NOT NULL,
    discount_minor        bigint        NOT NULL,
    gst_rate              numeric(5,2)  NOT NULL,
    taxable_value_minor   bigint        NOT NULL,
    cgst_minor            bigint        NOT NULL,
    sgst_minor            bigint        NOT NULL,
    igst_minor            bigint        NOT NULL,
    line_total_minor      bigint        NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_invoice_items_invoice
    ON invoice_items (tenant_id, invoice_id);

CREATE TABLE IF NOT EXISTS gst_tax_lines (
    id                    char(26)     PRIMARY KEY,
    tenant_id             char(26)     NOT NULL,
    invoice_id            char(26)     NOT NULL,
    gst_rate              numeric(5,2) NOT NULL,
    taxable_value_minor   bigint       NOT NULL,
    cgst_minor            bigint       NOT NULL,
    sgst_minor            bigint       NOT NULL,
    igst_minor            bigint       NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_gst_tax_lines_invoice
    ON gst_tax_lines (tenant_id, invoice_id);

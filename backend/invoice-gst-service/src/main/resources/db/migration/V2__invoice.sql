-- Sprint 4 invoice & GST schema. All document types (tax invoice, bill of supply, receipt voucher,
-- credit note, debit note) share one `invoices` table keyed by document_type; notes reference the
-- original via original_document_id.

CREATE TABLE invoices (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    document_type       text        NOT NULL,               -- TAX_INVOICE, BILL_OF_SUPPLY, RECEIPT_VOUCHER, CREDIT_NOTE, DEBIT_NOTE
    supply_type         text        NOT NULL,               -- B2B, B2C
    customer_id         char(26),
    customer_name       text,
    customer_gstin      text,
    place_of_supply     char(2)     NOT NULL,               -- GST state code
    business_state_code char(2)     NOT NULL,
    invoice_number      text        NOT NULL,
    financial_year      text        NOT NULL,               -- e.g. 2026-27
    invoice_date        date        NOT NULL,
    original_document_id char(26),                           -- for credit/debit notes
    reason              text,
    reverse_charge      boolean     NOT NULL DEFAULT false,
    taxable             boolean     NOT NULL DEFAULT true,   -- false for bill of supply / composition
    total_taxable_minor bigint      NOT NULL DEFAULT 0,
    total_cgst_minor    bigint      NOT NULL DEFAULT 0,
    total_sgst_minor    bigint      NOT NULL DEFAULT 0,
    total_igst_minor    bigint      NOT NULL DEFAULT 0,
    total_tax_minor     bigint      NOT NULL DEFAULT 0,
    total_amount_minor  bigint      NOT NULL DEFAULT 0,
    status              text        NOT NULL DEFAULT 'ISSUED', -- ISSUED, SENT, CANCELLED
    payment_request_id  char(26),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_invoice_number ON invoices (tenant_id, document_type, financial_year, invoice_number);
CREATE INDEX ix_invoice_tenant_date ON invoices (tenant_id, invoice_date DESC);

CREATE TABLE invoice_items (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    invoice_id         char(26)    NOT NULL REFERENCES invoices(id),
    product_id         char(26),
    description        text        NOT NULL,
    hsn_sac            text,
    quantity           numeric(18,3) NOT NULL,
    unit_price_minor   bigint      NOT NULL,
    discount_minor     bigint      NOT NULL DEFAULT 0,
    gst_rate           numeric(5,2) NOT NULL DEFAULT 0,
    taxable_value_minor bigint     NOT NULL,
    cgst_minor         bigint      NOT NULL DEFAULT 0,
    sgst_minor         bigint      NOT NULL DEFAULT 0,
    igst_minor         bigint      NOT NULL DEFAULT 0,
    line_total_minor   bigint      NOT NULL
);
CREATE INDEX ix_invoice_items_invoice ON invoice_items (invoice_id);

CREATE TABLE gst_tax_lines (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    invoice_id          char(26)    NOT NULL REFERENCES invoices(id),
    gst_rate            numeric(5,2) NOT NULL,
    taxable_value_minor bigint      NOT NULL,
    cgst_minor          bigint      NOT NULL DEFAULT 0,
    sgst_minor          bigint      NOT NULL DEFAULT 0,
    igst_minor          bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_gst_tax_lines_invoice ON gst_tax_lines (invoice_id);

-- Per-tenant, per-financial-year document number sequences (concurrency-safe via row lock).
CREATE TABLE document_sequences (
    tenant_id      char(26) NOT NULL,
    doc_type       text     NOT NULL,
    financial_year text     NOT NULL,
    prefix         text     NOT NULL,
    next_number    bigint   NOT NULL DEFAULT 1,
    PRIMARY KEY (tenant_id, doc_type, financial_year)
);

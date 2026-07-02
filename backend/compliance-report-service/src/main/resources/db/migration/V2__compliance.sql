-- Sprint 12 compliance schema. Source records are a period read-model built from upstream events;
-- reports are generated from them and governed by a status lifecycle. A report is only FILED with an
-- official portal/API acknowledgement — never inferred.

CREATE TABLE source_records (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    record_type   text        NOT NULL,                 -- SALES, PURCHASE, PAYROLL
    year          int         NOT NULL,
    month         int         NOT NULL,
    taxable_minor bigint      NOT NULL DEFAULT 0,
    tax_minor     bigint      NOT NULL DEFAULT 0,        -- output GST (SALES) or input GST/ITC (PURCHASE)
    statutory_minor bigint    NOT NULL DEFAULT 0,        -- PF/ESI/PT (PAYROLL)
    tds_minor     bigint      NOT NULL DEFAULT 0,
    supply_type   text,                                  -- B2B/B2C for sales
    source_ref    char(26)    NOT NULL,
    reference     text,                                  -- invoice/bill/run number
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, record_type, source_ref)
);
CREATE INDEX ix_source_period ON source_records (tenant_id, record_type, year, month);

CREATE TABLE compliance_reports (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    type               text        NOT NULL,             -- GSTR1_SUMMARY, GSTR3B_SUMMARY, SALES_REGISTER, PURCHASE_REGISTER, ITC_SUMMARY, TDS_SUMMARY, PAYROLL_COMPLIANCE, ITR_SUMMARY
    year               int         NOT NULL,
    month              int         NOT NULL,
    status             text        NOT NULL DEFAULT 'DRAFT', -- DRAFT, REVIEWED, APPROVED, FILED
    data_reconciled    boolean     NOT NULL DEFAULT false,
    total_taxable_minor bigint     NOT NULL DEFAULT 0,
    total_tax_minor    bigint      NOT NULL DEFAULT 0,
    net_payable_minor  bigint      NOT NULL DEFAULT 0,
    missing_fields     jsonb       NOT NULL DEFAULT '[]',
    ack_reference      text,                              -- official portal/API acknowledgement (FILED)
    generated_at       timestamptz NOT NULL DEFAULT now(),
    reviewed_by        char(26),
    approved_by        char(26),
    filed_at           timestamptz,
    version            bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_report_period ON compliance_reports (tenant_id, type, year, month);

CREATE TABLE compliance_report_lines (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    report_id     char(26)    NOT NULL REFERENCES compliance_reports(id),
    label         text        NOT NULL,
    taxable_minor bigint      NOT NULL DEFAULT 0,
    tax_minor     bigint      NOT NULL DEFAULT 0,
    amount_minor  bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_report_lines_report ON compliance_report_lines (report_id);

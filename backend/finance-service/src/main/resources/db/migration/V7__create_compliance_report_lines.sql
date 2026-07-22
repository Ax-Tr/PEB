-- V7: Create compliance_report_lines table for the compliance-report module.
CREATE TABLE compliance_report_lines (
    id            char(26) PRIMARY KEY,
    tenant_id     char(26) NOT NULL,
    report_id     char(26) NOT NULL,
    label         text     NOT NULL,
    taxable_minor bigint   NOT NULL,
    tax_minor     bigint   NOT NULL,
    amount_minor  bigint   NOT NULL
);

CREATE INDEX ix_compliance_report_lines_report ON compliance_report_lines (report_id);

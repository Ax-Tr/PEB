-- V6: Create source_records table for the compliance-report module.
CREATE TABLE source_records (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    record_type     text        NOT NULL,
    year            int         NOT NULL,
    month           int         NOT NULL,
    taxable_minor   bigint      NOT NULL,
    tax_minor       bigint      NOT NULL,
    statutory_minor bigint      NOT NULL,
    tds_minor       bigint      NOT NULL,
    supply_type     text,
    source_ref      char(26)    NOT NULL,
    reference       text,
    created_at      timestamptz NOT NULL
);

CREATE INDEX ix_source_records_period ON source_records (tenant_id, year, month);

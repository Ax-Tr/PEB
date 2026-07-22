-- V4: Align compliance_reports table with the current ComplianceReport entity.
-- The entity was refactored to use simpler column names (type/year/month) and
-- add financial summary columns (totals, missing_fields, ack_reference, reviewed_by, approved_by).

ALTER TABLE compliance_reports
  ADD COLUMN IF NOT EXISTS type              text,
  ADD COLUMN IF NOT EXISTS year              integer,
  ADD COLUMN IF NOT EXISTS month             integer,
  ADD COLUMN IF NOT EXISTS data_reconciled   boolean  NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS total_taxable_minor bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_tax_minor   bigint   NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS net_payable_minor bigint   NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS missing_fields    jsonb    NOT NULL DEFAULT '[]',
  ADD COLUMN IF NOT EXISTS ack_reference     text,
  ADD COLUMN IF NOT EXISTS reviewed_by       char(26),
  ADD COLUMN IF NOT EXISTS approved_by       char(26);

-- Back-fill new alias columns from the old ones for any existing rows.
UPDATE compliance_reports SET type  = report_type   WHERE type  IS NULL;
UPDATE compliance_reports SET year  = period_year   WHERE year  IS NULL;
UPDATE compliance_reports SET month = period_month  WHERE month IS NULL;

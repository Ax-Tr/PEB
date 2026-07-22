-- V8: Drop NOT NULL constraints on legacy columns of compliance_reports table.
-- The refactored ComplianceReport entity maps to the new 'type' and 'year' columns,
-- so the legacy columns 'report_type' and 'period_year' will receive null values
-- on new inserts, which previously triggered not-null constraint violations.

ALTER TABLE compliance_reports ALTER COLUMN report_type DROP NOT NULL;
ALTER TABLE compliance_reports ALTER COLUMN period_year DROP NOT NULL;

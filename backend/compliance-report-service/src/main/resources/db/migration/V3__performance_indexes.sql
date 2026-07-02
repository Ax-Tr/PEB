-- Sprint 17 performance index. The report list endpoint sorts by generated_at with only a tenant
-- filter (findByTenantIdOrderByGeneratedAtDesc); the existing (tenant_id, type, year, month) index
-- does not serve that ordering. This covering index makes the list a range scan on the leading keys.
CREATE INDEX ix_report_tenant_generated ON compliance_reports (tenant_id, generated_at DESC);

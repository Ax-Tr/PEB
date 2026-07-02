-- Sprint 17 performance indexes. The unfiltered list endpoints order by created_at with only a
-- tenant filter (findByTenantIdOrderByCreatedAtDesc); the existing (tenant_id, status, created_at)
-- composites cannot serve a tenant-only + order-by-created_at scan efficiently (status sits between
-- the filter and the sort key). These leading (tenant_id, created_at) indexes cover the "list all"
-- path; the status-filtered lists continue to use the composite indexes.
CREATE INDEX ix_ai_suggestions_tenant_created ON ai_suggestions (tenant_id, created_at DESC);
CREATE INDEX ix_anomaly_alerts_tenant_created ON anomaly_alerts (tenant_id, created_at DESC);

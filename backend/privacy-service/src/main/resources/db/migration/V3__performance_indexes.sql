-- Sprint 17 performance index. The DSR list (findByTenantIdOrderByReceivedAtDesc) orders by
-- received_at with only a tenant filter; the (tenant_id, status, received_at) composite does not
-- serve that scan (status sits between the filter and the sort key). This covers the "list all" path.
CREATE INDEX ix_dsr_tenant_received ON dsr_requests (tenant_id, received_at DESC);

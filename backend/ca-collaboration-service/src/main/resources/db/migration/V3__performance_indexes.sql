-- Sprint 17 performance index. The invite list (findByTenantIdOrderByCreatedAtDesc) orders by
-- created_at with only a tenant filter; the existing (tenant_id, status) index does not serve that
-- ordering. This index makes the listing a leading-key range scan.
CREATE INDEX ix_ca_invites_tenant_created ON ca_invites (tenant_id, created_at DESC);

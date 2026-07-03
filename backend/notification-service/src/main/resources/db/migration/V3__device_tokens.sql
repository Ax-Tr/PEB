-- Push device tokens: lets notification-service target a user's device(s) for push. One row per
-- (tenant, token); a token re-registered is reactivated + touched (idempotent). Tokens are revoked
-- (soft) on logout/unregister rather than hard-deleted, keeping an auditable record.
CREATE TABLE device_tokens (
    id           char(26)    PRIMARY KEY,
    tenant_id    char(26)    NOT NULL,
    user_id      char(26),
    token        text        NOT NULL,
    platform     text        NOT NULL,          -- ios | android | web
    active       boolean     NOT NULL DEFAULT true,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, token)
);
CREATE INDEX ix_device_tokens_tenant_active ON device_tokens (tenant_id, active, updated_at DESC);
CREATE INDEX ix_device_tokens_user ON device_tokens (tenant_id, user_id);

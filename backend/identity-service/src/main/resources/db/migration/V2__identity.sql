-- Sprint 1 identity schema: users, RBAC, devices, sessions, OTP audit, DPDP consent.

CREATE TABLE roles (
    id          char(26)    PRIMARY KEY,
    name        text        NOT NULL UNIQUE,          -- OWNER, CASHIER, ACCOUNTANT, ...
    description text        NOT NULL DEFAULT '',
    system_role boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id          char(26)    PRIMARY KEY,
    code        text        NOT NULL UNIQUE,          -- e.g. ledger:read, payout:approve
    description text        NOT NULL DEFAULT ''
);

CREATE TABLE role_permissions (
    role_id       char(26) NOT NULL REFERENCES roles(id),
    permission_id char(26) NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26),                              -- set once the user's business is created
    mobile_enc  text        NOT NULL,                  -- AES-GCM ciphertext (field-level encryption)
    mobile_hash char(64)    NOT NULL UNIQUE,           -- HMAC blind index for lookup (one phone = one user)
    email_enc   text,
    display_name text,
    status      text        NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_users_tenant ON users (tenant_id);

CREATE TABLE user_roles (
    user_id   char(26) NOT NULL REFERENCES users(id),
    role_id   char(26) NOT NULL REFERENCES roles(id),
    tenant_id char(26),                                -- role scoped to a business
    granted_at timestamptz NOT NULL DEFAULT now(),
    granted_by char(26),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE devices (
    id            char(26)    PRIMARY KEY,
    user_id       char(26)    NOT NULL REFERENCES users(id),
    device_hash   char(64)    NOT NULL,                -- HMAC of device fingerprint
    platform      text,                                 -- ANDROID, IOS, WEB
    model         text,
    last_seen_at  timestamptz NOT NULL DEFAULT now(),
    trusted       boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, device_hash)
);

CREATE TABLE user_sessions (
    id                 char(26)    PRIMARY KEY,
    user_id            char(26)    NOT NULL REFERENCES users(id),
    device_id          char(26)    REFERENCES devices(id),
    refresh_token_hash char(64)    NOT NULL UNIQUE,    -- SHA-256 of the opaque refresh token
    family_id          char(26)    NOT NULL,           -- rotation family; reuse of a rotated token revokes the family
    status             text        NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ROTATED, REVOKED, EXPIRED
    ip                 text,
    user_agent         text,
    issued_at          timestamptz NOT NULL DEFAULT now(),
    expires_at         timestamptz NOT NULL,
    revoked_at         timestamptz
);
CREATE INDEX ix_sessions_user ON user_sessions (user_id, status);

-- OTP live state is in Redis; this table is an append-only audit/rate-history record.
CREATE TABLE otp_requests (
    id          char(26)    PRIMARY KEY,
    mobile_hash char(64)    NOT NULL,
    purpose     text        NOT NULL,                  -- LOGIN, STEP_UP
    status      text        NOT NULL,                  -- ISSUED, VERIFIED, FAILED, EXPIRED
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_otp_mobile_time ON otp_requests (mobile_hash, created_at DESC);

CREATE TABLE data_consent_records (
    id          char(26)    PRIMARY KEY,
    user_id     char(26)    NOT NULL REFERENCES users(id),
    purpose     text        NOT NULL,                  -- e.g. ACCOUNT_CREATION, MARKETING
    version     text        NOT NULL,                  -- notice version consented to
    granted     boolean     NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_consent_user ON data_consent_records (user_id);

-- Seed system roles. IDs are deterministic, exactly-26-char tokens (rpad) so they are stable
-- across environments and safe in a char(26) column without padding surprises.
INSERT INTO roles (id, name, description) VALUES
  (rpad('ROLE_OWNER',          26, '.'), 'OWNER',          'Business owner — full access'),
  (rpad('ROLE_CO_OWNER',       26, '.'), 'CO_OWNER',       'Co-owner'),
  (rpad('ROLE_CASHIER',        26, '.'), 'CASHIER',        'Receive/Pay only'),
  (rpad('ROLE_ACCOUNTANT',     26, '.'), 'ACCOUNTANT',     'Ledger and reports'),
  (rpad('ROLE_CA',             26, '.'), 'CA',             'Chartered accountant reviewer'),
  (rpad('ROLE_AUDITOR',        26, '.'), 'AUDITOR',        'Read-only evidence'),
  (rpad('ROLE_EMPLOYEE',       26, '.'), 'EMPLOYEE',       'Self-service'),
  (rpad('ROLE_BRANCH_MANAGER', 26, '.'), 'BRANCH_MANAGER', 'Branch operations'),
  (rpad('ROLE_SUPPORT_ADMIN',  26, '.'), 'SUPPORT_ADMIN',  'Support desk'),
  (rpad('ROLE_SUPER_ADMIN',    26, '.'), 'SUPER_ADMIN',    'Platform administrator');

-- A minimal permission set; expanded per sprint as features land.
INSERT INTO permissions (id, code, description) VALUES
  (rpad('PERM_business_manage', 26, '.'), 'business:manage',  'Create/update business profile & settings'),
  (rpad('PERM_user_manage',     26, '.'), 'user:manage',      'Invite users and assign roles'),
  (rpad('PERM_receive_create',  26, '.'), 'receive:create',   'Collect payments'),
  (rpad('PERM_pay_create',      26, '.'), 'pay:create',       'Make payouts'),
  (rpad('PERM_ledger_read',     26, '.'), 'ledger:read',      'View ledger and reports'),
  (rpad('PERM_compliance_read', 26, '.'), 'compliance:read',  'View compliance reports');

-- OWNER gets everything seeded so far.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'OWNER';

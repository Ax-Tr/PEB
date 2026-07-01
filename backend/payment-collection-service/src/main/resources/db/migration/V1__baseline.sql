-- Sprint 3 baseline for payment_db: shared reliability + audit infrastructure.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE outbox_events (
    id             char(26)    PRIMARY KEY,
    aggregate_type text        NOT NULL,
    aggregate_id   char(26)    NOT NULL,
    event_type     text        NOT NULL,
    event_version  int         NOT NULL DEFAULT 1,
    tenant_id      char(26)    NOT NULL,
    payload        jsonb       NOT NULL,
    headers        jsonb       NOT NULL DEFAULT '{}',
    created_at     timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz,
    attempts       int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    event_id     char(26)    NOT NULL,
    consumer     text        NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);

CREATE TABLE idempotency_keys (
    tenant_id    char(26)    NOT NULL,
    key          text        NOT NULL,
    endpoint     text        NOT NULL,
    request_hash text        NOT NULL,
    status       text        NOT NULL,
    response     jsonb,
    created_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, key)
);

CREATE TABLE audit_events (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    actor_id       char(26),
    event_type     text        NOT NULL,
    entity_type    text,
    entity_id      char(26),
    correlation_id text,
    occurred_at    timestamptz NOT NULL DEFAULT now(),
    data           jsonb       NOT NULL DEFAULT '{}'
);
CREATE INDEX ix_audit_tenant_time ON audit_events (tenant_id, occurred_at DESC);

-- V3: Create commitment_events table for tracking commitment lifecycle events.

CREATE TABLE IF NOT EXISTS commitment_events (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    commitment_id char(26)    NOT NULL,
    event_type    text        NOT NULL,
    old_due_date  date,
    new_due_date  date,
    amount_minor  bigint,
    note          text,
    actor_id      char(26),
    occurred_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_commitment_event_ref ON commitment_events (tenant_id, commitment_id);

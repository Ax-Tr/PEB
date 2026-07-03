-- Sprint 24: commitment analytics projection, fed only by commitment.events.
CREATE TABLE fact_commitments (
    tenant_id          char(26)    NOT NULL,
    commitment_id      char(26)    NOT NULL,
    counterparty_type  text        NOT NULL,
    counterparty_id    char(26),
    counterparty_name  text,
    source_type        text,
    due_date           date        NOT NULL,
    amount_minor       bigint      NOT NULL DEFAULT 0,
    paid_minor         bigint      NOT NULL DEFAULT 0,
    outstanding_minor  bigint      NOT NULL DEFAULT 0,
    status             text        NOT NULL,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (commitment_id)
);
CREATE INDEX ix_fact_commitments_tenant_due ON fact_commitments (tenant_id, due_date, status);
CREATE INDEX ix_fact_commitments_tenant_status ON fact_commitments (tenant_id, status);

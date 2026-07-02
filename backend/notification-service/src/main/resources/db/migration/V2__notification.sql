-- Sprint 9 notification schema: templates, delivery logs, scheduled reminders.

CREATE TABLE notification_templates (
    id         char(26)    PRIMARY KEY,
    tenant_id  char(26)    NOT NULL,
    code       text        NOT NULL,                 -- e.g. EMI_DUE_REMINDER
    channel    text        NOT NULL,                 -- SMS, EMAIL, PUSH, WHATSAPP
    subject    text,                                  -- used by EMAIL/PUSH
    body       text        NOT NULL,                  -- with {{placeholders}}
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code, channel)
);

CREATE TABLE notification_logs (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    channel       text        NOT NULL,
    recipient     text        NOT NULL,               -- masked mobile/email in logs by convention
    template_code text,
    subject       text,
    body          text        NOT NULL,
    status        text        NOT NULL,               -- QUEUED, SENT, DELIVERED, FAILED
    provider      text,
    provider_ref  text,
    failure_reason text,
    attempts      int         NOT NULL DEFAULT 0,
    reminder_id   char(26),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_notif_tenant_time ON notification_logs (tenant_id, created_at DESC);
CREATE INDEX ix_notif_provider_ref ON notification_logs (provider, provider_ref);

CREATE TABLE reminder_schedules (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    source_type   text,                                -- INSTALLMENT_EMI, MANUAL
    source_ref    char(26),                            -- e.g. installment id
    emi_number    int,
    channel       text        NOT NULL,
    template_code text        NOT NULL,
    recipient     text        NOT NULL,
    variables     jsonb       NOT NULL DEFAULT '{}',
    due_date      date        NOT NULL,                -- the underlying due date
    send_on       date        NOT NULL,                -- when to fire (due - offset)
    offset_days   int         NOT NULL,                -- 3, 1, 0 (D-3/D-1/D-day)
    status        text        NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, SENT, CANCELLED
    created_at    timestamptz NOT NULL DEFAULT now(),
    sent_at       timestamptz
);
CREATE INDEX ix_reminder_due ON reminder_schedules (tenant_id, send_on) WHERE status = 'SCHEDULED';
-- Dedupe: one reminder per (source, emi, offset).
CREATE UNIQUE INDEX ux_reminder_source ON reminder_schedules (tenant_id, source_ref, emi_number, offset_days)
    WHERE source_ref IS NOT NULL;

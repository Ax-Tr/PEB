-- Employee master + salary structure. Sensitive fields (mobile/email/pan) are stored encrypted
-- (application-side AES-GCM) in the *_enc columns. Monetary amounts are integer paise (*_minor).

CREATE TABLE employees (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    name            text        NOT NULL,
    mobile_enc      text,
    email_enc       text,
    pan_enc         text,
    designation     text,
    date_of_joining date,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_employee_tenant ON employees (tenant_id, created_at DESC);

CREATE TABLE salary_structures (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    employee_id        char(26)    NOT NULL REFERENCES employees (id),
    gross_salary_minor bigint      NOT NULL,
    basic_minor        bigint      NOT NULL DEFAULT 0,
    hra_minor          bigint      NOT NULL DEFAULT 0,
    pf_applicable      boolean     NOT NULL DEFAULT false,
    esi_applicable     boolean     NOT NULL DEFAULT false,
    pt_applicable      boolean     NOT NULL DEFAULT false,
    effective_from     date,
    version            bigint      NOT NULL DEFAULT 0,
    UNIQUE (employee_id)
);

-- Sprint 7 payroll run schema. One run per tenant per (year, month) — idempotent salary processing.

CREATE TABLE salary_runs (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    year              int         NOT NULL,
    month             int         NOT NULL,                 -- 1..12
    working_days      int         NOT NULL,
    status            text        NOT NULL DEFAULT 'DRAFT', -- DRAFT, PROCESSED
    total_earnings_minor   bigint NOT NULL DEFAULT 0,
    total_net_minor        bigint NOT NULL DEFAULT 0,
    total_statutory_minor  bigint NOT NULL DEFAULT 0,       -- PF + ESI + PT + other withholdings
    total_tds_minor        bigint NOT NULL DEFAULT 0,
    employee_count    int         NOT NULL DEFAULT 0,
    created_at        timestamptz NOT NULL DEFAULT now(),
    created_by        char(26),
    version           bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, year, month)
);

CREATE TABLE salary_run_lines (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    salary_run_id       char(26)    NOT NULL REFERENCES salary_runs(id),
    employee_id         char(26)    NOT NULL,
    gross_minor         bigint      NOT NULL,
    basic_minor         bigint      NOT NULL,
    lop_days            int         NOT NULL DEFAULT 0,
    earned_gross_minor  bigint      NOT NULL,
    incentives_minor    bigint      NOT NULL DEFAULT 0,
    pf_minor            bigint      NOT NULL DEFAULT 0,
    esi_minor           bigint      NOT NULL DEFAULT 0,
    pt_minor            bigint      NOT NULL DEFAULT 0,
    tds_minor           bigint      NOT NULL DEFAULT 0,
    other_deductions_minor bigint   NOT NULL DEFAULT 0,
    net_pay_minor       bigint      NOT NULL,
    payslip_document_id char(26)                             -- set when the payslip PDF is generated
);
CREATE INDEX ix_salary_run_lines_run ON salary_run_lines (salary_run_id);
CREATE INDEX ix_salary_run_lines_employee ON salary_run_lines (tenant_id, employee_id);

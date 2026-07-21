-- Consolidated business-service baseline schema.
-- Merges: customer, vendor, employee-payroll, product, invoice-gst, purchase-expense,
--         commitment, notification, ocr-document, ca-collaboration.

-- ===== OUTBOX (shared by all modules for transactional outbox pattern) =====
CREATE TABLE outbox_events (
    id              char(26)    PRIMARY KEY,
    aggregate_type  text        NOT NULL,
    aggregate_id    char(26)    NOT NULL,
    event_type      text        NOT NULL,
    event_version   int         NOT NULL DEFAULT 1,
    tenant_id       char(26)    NOT NULL,
    payload         jsonb       NOT NULL,
    headers         jsonb       NOT NULL DEFAULT '{}',
    created_at      timestamptz NOT NULL DEFAULT now(),
    published_at    timestamptz,
    attempts        int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- ===== IDEMPOTENCY =====
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

-- ===== AUDIT =====
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

-- ===== CUSTOMER MODULE =====
CREATE TABLE customers (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    mobile_enc  text        NOT NULL,
    mobile_hash char(64)    NOT NULL,
    email_enc   text,
    address     text,
    gstin_enc   text,
    status      text        NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, mobile_hash)
);
CREATE INDEX ix_customer_tenant ON customers (tenant_id, created_at DESC);

CREATE TABLE customer_contacts (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    customer_id char(26)    NOT NULL REFERENCES customers(id),
    type        text        NOT NULL,
    value_enc   text        NOT NULL,
    preferred   boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_customer_contacts_customer ON customer_contacts (customer_id);

-- ===== VENDOR MODULE =====
CREATE TABLE vendors (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    mobile_enc  text,
    email_enc   text,
    gstin_enc   text,
    address     text,
    status      text        NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_vendor_tenant ON vendors (tenant_id, created_at DESC);

CREATE TABLE vendor_bank_accounts (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    vendor_id           char(26)    NOT NULL REFERENCES vendors (id),
    account_number_enc  text        NOT NULL,
    account_number_hash char(64)    NOT NULL,
    ifsc_enc            text        NOT NULL,
    upi_enc             text,
    bank_name           text        NOT NULL,
    holder_name         text        NOT NULL,
    status              text        NOT NULL DEFAULT 'PENDING_REVIEW',
    source              text        NOT NULL,
    reviewed_by         char(26),
    reviewed_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (vendor_id, account_number_hash)
);
CREATE INDEX ix_vendor_bank_accounts_vendor ON vendor_bank_accounts (vendor_id);

-- ===== EMPLOYEE-PAYROLL MODULE =====
CREATE TABLE employees (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    employee_code   text        NOT NULL,
    name            text        NOT NULL,
    email_enc       text,
    mobile_enc      text,
    designation     text,
    department      text,
    ctc_minor       bigint,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, employee_code)
);
CREATE INDEX ix_employee_tenant ON employees (tenant_id, status);

CREATE TABLE salary_runs (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    period_year     int         NOT NULL,
    period_month    int         NOT NULL,
    status          text        NOT NULL DEFAULT 'DRAFT',
    total_minor     bigint      NOT NULL DEFAULT 0,
    run_by          char(26),
    created_at      timestamptz NOT NULL DEFAULT now(),
    finalized_at    timestamptz,
    version         bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, period_year, period_month)
);

CREATE TABLE salary_slips (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    salary_run_id   char(26)    NOT NULL REFERENCES salary_runs(id),
    employee_id     char(26)    NOT NULL REFERENCES employees(id),
    gross_minor     bigint      NOT NULL,
    deductions_minor bigint     NOT NULL DEFAULT 0,
    net_minor       bigint      NOT NULL,
    status          text        NOT NULL DEFAULT 'PENDING',
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_salary_slips_run ON salary_slips (salary_run_id);

-- ===== PRODUCT MODULE =====
CREATE TABLE products (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    name        text        NOT NULL,
    sku         text,
    hsn_sac     text,
    unit        text        NOT NULL DEFAULT 'NOS',
    rate_minor  bigint      NOT NULL,
    gst_rate    numeric(5,2) NOT NULL DEFAULT 0,
    description text,
    status      text        NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_product_tenant ON products (tenant_id, status);
CREATE UNIQUE INDEX ux_product_sku ON products (tenant_id, sku) WHERE sku IS NOT NULL;

-- ===== INVOICE-GST MODULE =====
CREATE TABLE invoices (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    invoice_number      text        NOT NULL,
    customer_id         char(26),
    status              text        NOT NULL DEFAULT 'DRAFT',
    issue_date          date        NOT NULL,
    due_date            date,
    subtotal_minor      bigint      NOT NULL DEFAULT 0,
    discount_minor      bigint      NOT NULL DEFAULT 0,
    tax_minor           bigint      NOT NULL DEFAULT 0,
    total_minor         bigint      NOT NULL DEFAULT 0,
    amount_paid_minor   bigint      NOT NULL DEFAULT 0,
    place_of_supply     char(2),
    reverse_charge      boolean     NOT NULL DEFAULT false,
    notes               text,
    terms               text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, invoice_number)
);
CREATE INDEX ix_invoice_tenant ON invoices (tenant_id, issue_date DESC);
CREATE INDEX ix_invoice_customer ON invoices (tenant_id, customer_id) WHERE customer_id IS NOT NULL;

CREATE TABLE invoice_line_items (
    id              char(26)    PRIMARY KEY,
    invoice_id      char(26)    NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    tenant_id       char(26)    NOT NULL,
    product_id      char(26),
    description     text        NOT NULL,
    hsn_sac         text,
    quantity        numeric(12,3) NOT NULL DEFAULT 1,
    unit            text        NOT NULL DEFAULT 'NOS',
    rate_minor      bigint      NOT NULL,
    discount_minor  bigint      NOT NULL DEFAULT 0,
    taxable_minor   bigint      NOT NULL,
    cgst_minor      bigint      NOT NULL DEFAULT 0,
    sgst_minor      bigint      NOT NULL DEFAULT 0,
    igst_minor      bigint      NOT NULL DEFAULT 0,
    cess_minor      bigint      NOT NULL DEFAULT 0,
    total_minor     bigint      NOT NULL,
    sort_order      int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_invoice_line_items ON invoice_line_items (invoice_id, sort_order);

-- ===== PURCHASE-EXPENSE MODULE =====
CREATE TABLE purchase_bills (
    id                  char(26)    PRIMARY KEY,
    tenant_id           char(26)    NOT NULL,
    bill_number         text        NOT NULL,
    vendor_id           char(26),
    status              text        NOT NULL DEFAULT 'RECORDED',
    bill_date           date        NOT NULL,
    due_date            date,
    subtotal_minor      bigint      NOT NULL DEFAULT 0,
    tax_minor           bigint      NOT NULL DEFAULT 0,
    total_minor         bigint      NOT NULL DEFAULT 0,
    amount_paid_minor   bigint      NOT NULL DEFAULT 0,
    place_of_supply     char(2),
    notes               text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, bill_number)
);
CREATE INDEX ix_purchase_bill_tenant ON purchase_bills (tenant_id, bill_date DESC);

CREATE TABLE purchase_line_items (
    id              char(26)    PRIMARY KEY,
    bill_id         char(26)    NOT NULL REFERENCES purchase_bills(id) ON DELETE CASCADE,
    tenant_id       char(26)    NOT NULL,
    description     text        NOT NULL,
    hsn_sac         text,
    quantity        numeric(12,3) NOT NULL DEFAULT 1,
    rate_minor      bigint      NOT NULL,
    taxable_minor   bigint      NOT NULL,
    cgst_minor      bigint      NOT NULL DEFAULT 0,
    sgst_minor      bigint      NOT NULL DEFAULT 0,
    igst_minor      bigint      NOT NULL DEFAULT 0,
    total_minor     bigint      NOT NULL,
    sort_order      int         NOT NULL DEFAULT 0
);

CREATE TABLE expenses (
    id                char(26)    PRIMARY KEY,
    tenant_id         char(26)    NOT NULL,
    category          text        NOT NULL,
    description       text,
    amount_minor      bigint      NOT NULL,
    expense_date      date        NOT NULL,
    payment_mode      text,
    vendor_id         char(26),
    receipt_doc_id    char(26),
    status            text        NOT NULL DEFAULT 'RECORDED',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_expense_tenant ON expenses (tenant_id, expense_date DESC);

-- ===== COMMITMENT MODULE =====
CREATE TABLE commitments (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    type            text        NOT NULL,
    counterparty_id char(26),
    counterparty_type text,
    description     text        NOT NULL,
    amount_minor    bigint      NOT NULL,
    start_date      date        NOT NULL,
    end_date        date,
    recurrence      text,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_commitment_tenant ON commitments (tenant_id, status, start_date);

-- ===== NOTIFICATION MODULE =====
CREATE TABLE notification_templates (
    id         char(26)    PRIMARY KEY,
    tenant_id  char(26)    NOT NULL,
    code       text        NOT NULL,
    channel    text        NOT NULL,
    subject    text,
    body       text        NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code, channel)
);

CREATE TABLE notification_logs (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    channel       text        NOT NULL,
    recipient     text        NOT NULL,
    template_code text,
    subject       text,
    body          text        NOT NULL,
    status        text        NOT NULL,
    provider      text,
    provider_ref  text,
    failure_reason text,
    attempts      int         NOT NULL DEFAULT 0,
    reminder_id   char(26),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_notif_tenant_time ON notification_logs (tenant_id, created_at DESC);

CREATE TABLE reminder_schedules (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    source_type   text,
    source_ref    char(26),
    emi_number    int,
    channel       text        NOT NULL,
    template_code text        NOT NULL,
    recipient     text        NOT NULL,
    variables     jsonb       NOT NULL DEFAULT '{}',
    due_date      date        NOT NULL,
    send_on       date        NOT NULL,
    offset_days   int         NOT NULL,
    status        text        NOT NULL DEFAULT 'SCHEDULED',
    created_at    timestamptz NOT NULL DEFAULT now(),
    sent_at       timestamptz
);
CREATE INDEX ix_reminder_due ON reminder_schedules (tenant_id, send_on) WHERE status = 'SCHEDULED';
CREATE UNIQUE INDEX ux_reminder_source ON reminder_schedules (tenant_id, source_ref, emi_number, offset_days)
    WHERE source_ref IS NOT NULL;

-- ===== OCR-DOCUMENT MODULE =====
CREATE TABLE documents (
    id                char(26)     PRIMARY KEY,
    tenant_id         char(26)     NOT NULL,
    storage_key       text         NOT NULL,
    original_filename text         NOT NULL,
    mime_type         text         NOT NULL,
    checksum          text,
    size_bytes        bigint       NOT NULL,
    uploaded_by       char(26),
    created_at        timestamptz  NOT NULL,
    CONSTRAINT ck_documents_size_positive CHECK (size_bytes > 0)
);
CREATE UNIQUE INDEX ux_documents_tenant_storage ON documents (tenant_id, storage_key);

CREATE TABLE ocr_jobs (
    id                    char(26)     PRIMARY KEY,
    tenant_id             char(26)     NOT NULL,
    document_id           char(26)     NOT NULL REFERENCES documents(id),
    document_type         text         NOT NULL,
    status                text         NOT NULL,
    raw_text_enc          text,
    extracted_fields_enc  text,
    confidence            numeric(5,4) NOT NULL DEFAULT 0,
    failure_reason        text,
    created_at            timestamptz  NOT NULL,
    updated_at            timestamptz  NOT NULL,
    reviewed_at           timestamptz,
    reviewed_by           char(26),
    CONSTRAINT ck_ocr_jobs_status CHECK (status IN ('QUEUED','PROCESSING','REVIEW_REQUIRED','COMPLETED','FAILED')),
    CONSTRAINT ck_ocr_jobs_type CHECK (document_type IN ('BANK_DETAILS','CHEQUE','PASSBOOK','INVOICE','RECEIPT')),
    CONSTRAINT ck_ocr_jobs_confidence CHECK (confidence >= 0 AND confidence <= 1)
);
CREATE INDEX ix_ocr_jobs_tenant ON ocr_jobs (tenant_id, created_at DESC);

-- ===== CA-COLLABORATION MODULE =====
CREATE TABLE ca_invites (
    id             char(26)    PRIMARY KEY,
    tenant_id      char(26)    NOT NULL,
    email_enc      text        NOT NULL,
    role           text        NOT NULL,
    status         text        NOT NULL DEFAULT 'PENDING',
    linked_user_id char(26),
    invited_by     char(26)    NOT NULL,
    expires_at     timestamptz NOT NULL,
    accepted_at    timestamptz,
    revoked_at     timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0
);
CREATE INDEX ix_ca_invites_tenant ON ca_invites (tenant_id, status);

CREATE TABLE review_notes (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    entity_type text        NOT NULL,
    entity_id   char(26)    NOT NULL,
    author_id   char(26)    NOT NULL,
    note        text        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_review_notes_entity ON review_notes (tenant_id, entity_type, entity_id, created_at);

CREATE TABLE report_approvals (
    id            char(26)    PRIMARY KEY,
    tenant_id     char(26)    NOT NULL,
    report_type   text        NOT NULL,
    report_ref    char(26)    NOT NULL,
    status        text        NOT NULL DEFAULT 'REQUESTED',
    requested_by  char(26)    NOT NULL,
    decided_by    char(26),
    decision_note text,
    requested_at  timestamptz NOT NULL DEFAULT now(),
    decided_at    timestamptz,
    version       bigint      NOT NULL DEFAULT 0
);

CREATE TABLE close_checklists (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    period_year int         NOT NULL,
    period_month int        NOT NULL,
    created_by  char(26)    NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, period_year, period_month)
);

CREATE TABLE close_checklist_items (
    id           char(26)    PRIMARY KEY,
    checklist_id char(26)    NOT NULL REFERENCES close_checklists (id),
    tenant_id    char(26)    NOT NULL,
    label        text        NOT NULL,
    mandatory    boolean     NOT NULL DEFAULT true,
    done         boolean     NOT NULL DEFAULT false,
    done_by      char(26),
    done_at      timestamptz,
    sort_order   int         NOT NULL DEFAULT 0
);

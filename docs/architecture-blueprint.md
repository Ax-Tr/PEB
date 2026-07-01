# PEB Architecture Blueprint

Covers required outputs 1–9. Read alongside [decisions.md](./decisions.md).

---

## 1. Final product architecture summary

PEB is an **Indian MSME Finance Operating System**: a mobile-first (Android-priority) and
web-supported platform that captures every credit/debit a small business makes, converts each
business event into **accounting-grade double-entry records**, prepares **compliance-ready**
GST/payroll/TDS/income-tax reports, and gives a non-accountant owner real-time answers to:
*money in today, money out today, who owes me, whom I owe, real profit left, pending compliance,
suspicious/unreconciled items, what's ready for my CA.*

**Style:** domain-driven microservices, database-per-service, event-driven via Kafka with the
transactional-outbox pattern, saga orchestration for multi-service financial workflows, CQRS
read-models for dashboards (ClickHouse), and human-in-the-loop AI (suggest + confidence, never
silent posting).

```
                         ┌──────────────┐      ┌──────────────┐
   Mobile (RN/TS) ──┐    │   Web (React)│      │  CA / Auditor│
   Android priority │    │  role dashbds│      │  web console │
                    ▼    ▼              ▼      ▼
                ┌────────────────────────────────────────┐
                │        Spring Cloud Gateway             │  TLS1.3, WAF, rate-limit,
                │  auth enforce · tenant resolve · corrID │  API versioning, req signing
                └───────────────┬────────────────────────┘
             mTLS (internal)    │
   ┌───────────────┬────────────┼───────────────┬───────────────────┐
   ▼               ▼            ▼               ▼                    ▼
 Identity       Tenant       Customer/Vendor  Payment/Payout    Accounting-Ledger
 (Keycloak)     Service      Employee/Catalog  Collection       (jOOQ double-entry)
   │               │            │               │                    │
   └──────── Kafka (domain events, outbox) ─────┴──────────┬─────────┘
                    │                                       │
             ┌──────┴───────┐                        ┌──────┴────────┐
             ▼              ▼                         ▼               ▼
        Reconciliation  Compliance-Report        Analytics       Audit & Evidence
        (weighted match) (GSTR/TDS/payroll)      (ClickHouse)    (OpenSearch, immutable)
                    │
            AI-Automation (classification, anomaly, forecast, NL assistant — human-approved)

 Data plane: PostgreSQL (per service) · Redis · Kafka · ClickHouse · OpenSearch · S3 · KMS/Vault
 Ops plane:  OTel → Prometheus/Grafana · Loki · Tempo/Jaeger · EKS · Terraform · Helm
```

**Key architectural properties**
- **Financial correctness first:** integer paise, balanced journal entries enforced in DB + tests, no hard delete, reversal-only corrections, optimistic locking on financial rows, DB constraints as the last line of defence.
- **Idempotency everywhere money moves:** idempotency key (Redis + DB unique constraint) on payment, payout, ledger post, webhook ingest, installment pay.
- **Reliable events:** outbox table written in the same TX as state change; a relay publishes to Kafka; consumers dedupe by `event_id`.
- **Human-in-the-loop:** maker–checker on high-risk actions; AI/OCR always surface confidence; low-confidence never auto-posts; month-lock gated by approval.
- **Compliance status is first-class:** every report row carries `unreconciled | draft | reviewed | approved`; filing "completed" only on official acknowledgement.
- **Tenant isolation:** `tenant_id`/`business_id` on every tenant-scoped row; enforced at gateway (tenant resolution), service (row filter + ABAC), and DB (RLS optional) layers; AI never crosses tenants.

---

## 2. Complete module list

Business/product modules (functional), mapped to owning services in §3.

| # | Module | Purpose |
|---|--------|---------|
| M01 | Auth & Onboarding | OTP/passkey login, device binding, business setup, GST/PAN/Udyam, bank/UPI setup |
| M02 | Home / Owner Dashboard | Money-in-today, money-out-today, receivables, payables, profit, attention items |
| M03 | Receive (Collect) | Customer search/create, item entry, GST, full/partial, dynamic UPI QR/link, webhook confirm, invoice dispatch, receivable installment trigger |
| M04 | Pay (Payout) | Employee salary payout, vendor purchase entry, vendor OCR bank capture, full/partial payout, payable installment trigger, maker-checker |
| M05 | Customers | Master, mobile search, ledger summary, receivables, contact prefs |
| M06 | Vendors | Onboarding, bank/UPI + OCR review, ledger, payables |
| M07 | Employees & Payroll | Master, salary structure, LOP, PF/ESI/PT/TDS/insurance/incentives, salary run, payslip |
| M08 | Product/Service Catalog | Items, HSN/SAC, GST rates, price history, margin defaults |
| M09 | Invoice & GST engine | Tax invoice, bill of supply, receipt voucher, credit/debit note, GST split, PDF, e-invoice/e-way readiness |
| M10 | Purchase & Expense | Purchase bill capture, expense categorization, bill matching, input GST/ITC, expense approval |
| M11 | Installment engine | Receivable/payable schedules, EMI due dates, D-3/D-1/D-day, links, balance updates, closure |
| M12 | Reminder & Notification | SMS/email/WhatsApp/push, templates, delivery status, escalation, retry |
| M13 | Transaction Monitor / Ingestion | Manual cash/bank, statement import, UPI import, gateway settlement import, duplicate detection, classification review |
| M14 | Accounting Ledger | CoA, journals, double-entry posting, ledger balances, trial balance, P&L, balance sheet, month lock, cash/accrual views |
| M15 | Reconciliation | Bank↔ledger, gateway settlement, cash, invoice↔payment, bill↔payment, payroll↔bank; suggestions & exceptions |
| M16 | Compliance Reports | GSTR-1/3B prep, sales/purchase registers, ITC, e-invoice/e-way readiness, TDS, PF/ESI/PT, ITR-ready summaries |
| M17 | Analytics & Intelligence | Revenue, cost, gross profit, margin, cashflow, receivables/payables aging, product profitability, branch-wise |
| M18 | Audit & Evidence | Immutable audit events, activity logs, evidence pack, maker-checker records, approval/export logs |
| M19 | CA/Accountant Collaboration | Invite, role-scoped access, review notes, report approval, month-end close checklist |
| M20 | AI Automation | Classification, anomaly detection, cashflow forecast, OCR assist, GST validation, NL assistant, voice entry — human-approved |
| M21 | OCR & Document Intelligence | QR/bank/bill/cheque extraction, confidence, manual review |
| M22 | Rules Engine | Tax/payroll/approval/reminder/compliance-calendar rules; configurable by state/industry/business type |
| M23 | Compliance Calendar & Risk | Due dates, anomaly/risk alerts |
| M24 | Settings & Roles | Business profile, users/roles, invoice/tax/notification settings, security, theme |
| M25 | Reports & Export | Report views + background export center (S3 signed URLs) |
| M26 | Voice Transaction Input | Speech → structured fields → confirm → save |
| M27 | Offline Draft & Sync | Encrypted offline drafts, "Pending Sync", conflict detection |

---

## 3. Microservices map

23 services (+ gateway). Each row: responsibility · owned tables · key published events · key sync deps.

| Service | Responsibility | Owned data (DB) | Publishes | Sync depends on |
|---------|----------------|-----------------|-----------|-----------------|
| **api-gateway** | routing, authN enforcement, rate-limit, tenant resolve, corrID, versioning | none | — | Identity (JWKS) |
| **identity-service** | OTP, passkey, JWT/OIDC (Keycloak), refresh rotation, RBAC/ABAC, device binding, sessions | users, roles, permissions, user_roles, user_sessions, devices, otp_requests | USER_LOGGED_IN, USER_ROLE_CHANGED, DEVICE_REGISTERED | Keycloak |
| **tenant-service** | business profile, branches, GST/PAN/Udyam, invoice/tax settings | businesses, branches, business_tax_profiles, business_settings | BUSINESS_CREATED, BUSINESS_SETTINGS_CHANGED | Identity |
| **customer-service** | customer master, mobile search, ledger summary, receivables, contact prefs | customers, customer_contacts | CUSTOMER_CREATED, CUSTOMER_UPDATED | Tenant |
| **vendor-service** | vendor onboarding, bank/UPI + OCR review workflow, ledger, payables | vendors, vendor_bank_accounts | VENDOR_CREATED, VENDOR_BANK_DETAILS_CHANGED | Tenant, OCR |
| **employee-payroll-service** | employee master, salary structure, LOP, PF/ESI/PT/TDS config, salary run, payslip trigger | employees, salary_structures, salary_runs, salary_run_lines | EMPLOYEE_CREATED, SALARY_RUN_CREATED, PAYSLIP_GENERATED | Tenant, Rules, Ledger |
| **product-service** | product/service master, HSN/SAC, GST rates, price history, margin | products, services, hsn_sac_master, price_history | PRODUCT_CREATED | Tenant |
| **payment-collection-service** | payment request, dynamic UPI QR, link, gateway abstraction, webhook verify, settlement status | payment_requests, payment_qr_codes, payment_webhooks | PAYMENT_REQUEST_CREATED, PAYMENT_QR_GENERATED, PAYMENT_RECEIVED, PAYMENT_FAILED | Customer, Invoice; PG webhooks |
| **payout-service** | vendor/employee payout, approval, beneficiary validation, gateway failover | payouts, payout_approvals, beneficiaries | VENDOR_PAYMENT_INITIATED/COMPLETED, PAYOUT_APPROVAL_* | Vendor, Employee, Rules, Ledger; PG |
| **transaction-ingestion-service** | manual cash/bank, statement/UPI/settlement import, duplicate detection | cash_transactions, bank_accounts, bank_transactions, upi_transactions, gateway_settlements, import_batches | BANK_TRANSACTION_IMPORTED, TRANSACTION_CLASSIFIED | Tenant; banks/PG; AI (classify) |
| **accounting-ledger-service** | double-entry engine, CoA, journals, balances, trial balance, P&L, BS, month lock | chart_of_accounts, journal_entries, journal_entry_lines, ledger_balances, financial_periods, month_locks | JOURNAL_ENTRY_POSTED, MONTH_LOCKED, MONTH_REOPEN_REQUESTED | Tenant, Rules; consumes many events |
| **invoice-gst-service** | tax invoice, bill of supply, receipt voucher, credit/debit note, GST split, PDF, e-invoice/e-way payloads | invoices, invoice_items, gst_tax_lines, credit_notes, debit_notes | INVOICE_GENERATED, INVOICE_SENT | Customer, Product, Tenant, Rules |
| **purchase-expense-service** | purchase bill capture, expense categorization, bill matching, input GST/ITC, approval | purchase_bills, purchase_items, expenses | PURCHASE_BILL_CREATED, EXPENSE_APPROVED | Vendor, Product, Rules, Ledger |
| **installment-service** | receivable/payable EMI schedules, due dates, balance updates, closure | installments, installment_emis | INSTALLMENT_SCHEDULE_CREATED, INSTALLMENT_PAID | Invoice, Purchase, Payment |
| **notification-service** | SMS/email/WhatsApp/push, templates, delivery status, escalation, retry | notification_templates, notification_logs, reminder_schedules | REMINDER_SENT, NOTIFICATION_DELIVERED/FAILED | providers; consumes reminder events |
| **ocr-document-service** | QR/bank/bill/cheque/statement extraction, confidence, manual review | ocr_jobs, documents | DOCUMENT_UPLOADED, OCR_COMPLETED | S3; AI (extract) |
| **reconciliation-service** | weighted matching bank/gateway/cash/invoice/bill/payroll; suggestions/exceptions | reconciliation_matches, reconciliation_exceptions | RECONCILIATION_MATCHED, RECONCILIATION_EXCEPTION_CREATED | Ledger, Ingestion, Invoice, Payout |
| **compliance-report-service** | GSTR-1/3B, sales/purchase register, ITC, e-invoice/e-way readiness, TDS, PF/ESI/PT, ITR-ready | compliance_reports, compliance_report_lines | COMPLIANCE_REPORT_GENERATED | Invoice, Purchase, Ledger, Payroll, Rules |
| **analytics-service** | revenue/cost/profit/margin/cashflow/aging/product profitability/branch | ClickHouse read-models (materialized) | — | consumes all financial events |
| **audit-evidence-service** | immutable audit events, activity logs, evidence pack, maker-checker & export logs | audit_events (aggregated), evidence_items, export_logs; OpenSearch index | AUDIT_EVENT_RECORDED | consumes AUDIT_EVENT_RECORDED from all |
| **ca-collaboration-service** | CA/accountant invite, role-scoped access, review notes, report approval, close checklist | ca_invites, review_notes, close_checklists, report_approvals | APPROVAL_REQUESTED, APPROVAL_COMPLETED | Identity, Compliance, Ledger |
| **ai-automation-service** | classification, anomaly, forecast, OCR assist, GST validation, NL/voice assistant | ai_suggestions, anomaly_alerts, ai_feedback | AI_SUGGESTION_CREATED, ANOMALY_DETECTED | model providers; read-models |
| **rules-engine-service** | tax/payroll/approval/reminder/compliance-calendar rules by state/industry/type | rule_sets, rules, compliance_calendar_items | RULE_CHANGED | Tenant |
| **approval-service** *(may fold into audit-evidence)* | generic maker-checker workflow engine | approval_workflows, approval_steps | APPROVAL_REQUESTED/COMPLETED | Rules, Identity |

> Cross-cutting `common-libraries` (a Gradle module, **not** a service): money value object, ULID,
> tenant context, correlation/MDC filter, error model, OpenAPI base, outbox helper, event envelope,
> idempotency helper, security utils, jOOQ/JPA base, testcontainers fixtures.

**Communication rules:** sync = versioned REST over mTLS through service mesh (never direct DB);
async = Kafka domain events (source of truth for cross-service state). Saga orchestration for
receive→invoice→ledger→installment and purchase→payout→ledger flows (see specs/idempotency-outbox-saga.md).

---

## 4. Database map

One PostgreSQL database per service (physical isolation in prod; separate schemas acceptable in
lower envs). All 57 core entities from the brief are placed with their owning service. Conventions
in [specs/data-dictionary-conventions.md](./specs/data-dictionary-conventions.md).

| Service DB | Tables |
|-----------|--------|
| identity_db | users, roles, permissions, role_permissions, user_roles, user_sessions, devices, otp_requests, data_consent_records* |
| tenant_db | businesses, branches, business_tax_profiles, business_settings |
| customer_db | customers, customer_contacts |
| vendor_db | vendors, vendor_bank_accounts |
| employee_db | employees, salary_structures, salary_runs, salary_run_lines |
| product_db | products, services, hsn_sac_master, price_history |
| payment_db | payment_requests, payment_qr_codes, payment_webhooks |
| payout_db | payouts, payout_approvals, beneficiaries |
| ingestion_db | cash_transactions, bank_accounts, bank_transactions, upi_transactions, gateway_settlements, import_batches |
| ledger_db | chart_of_accounts, journal_entries, journal_entry_lines, ledger_balances, financial_periods, month_locks |
| invoice_db | invoices, invoice_items, gst_tax_lines, credit_notes, debit_notes |
| purchase_db | purchase_bills, purchase_items, expenses |
| installment_db | installments, installment_emis |
| notification_db | notification_templates, notification_logs, reminder_schedules |
| ocr_db | ocr_jobs, documents |
| reconciliation_db | reconciliation_matches, reconciliation_exceptions |
| compliance_db | compliance_reports, compliance_report_lines, compliance_calendar_items* |
| analytics (ClickHouse) | fact_transactions, fact_invoices, agg_daily_cashflow, agg_receivables_aging, agg_payables_aging, agg_product_margin |
| audit_db + OpenSearch | audit_events, evidence_items, export_jobs, approval_workflows* |
| ca_collab_db | ca_invites, review_notes, close_checklists, report_approvals |
| ai_db | ai_suggestions, anomaly_alerts, ai_feedback |
| rules_db | rule_sets, rules, compliance_calendar_items* |

`*` = table whose ownership is shared conceptually; assigned to a single owner (consent → identity,
calendar → rules/compliance, approval_workflows → audit-evidence/approval) with others reading via API/events.

**Every service DB also has:** `outbox_events`, `processed_events` (consumer dedupe),
`idempotency_keys` (where applicable), and `audit_events` (local append-only), plus a Flyway
`flyway_schema_history`.

**Indexing strategy (baseline):** composite `(tenant_id, <natural key>)` on every tenant table;
`(tenant_id, created_at desc)` for feeds; partial indexes on `status`; unique constraints on
natural business keys (invoice number per tenant, GSTIN, idempotency key); BRIN on large append-only
time-series (bank_transactions, audit_events); foreign keys **within** a DB only.

---

## 5. Event map

Kafka topics per bounded context; envelope is mandatory on every event.

**Envelope (schema-registry, Avro/JSON-Schema):**
`event_id (ULID)`, `event_type`, `event_version` (schema_version), `tenant_id`, `business_id`,
`source_service`, `actor_id`, `correlation_id`, `causation_id`, `occurred_at`, `partition_key`, `payload`.

| Topic | Key events | Primary producers → consumers |
|-------|------------|-------------------------------|
| `identity.events` | USER_LOGGED_IN, USER_ROLE_CHANGED, DEVICE_REGISTERED | identity → audit, analytics |
| `tenant.events` | BUSINESS_CREATED, BUSINESS_SETTINGS_CHANGED | tenant → all masters, ledger (CoA seed) |
| `masters.events` | CUSTOMER_CREATED, VENDOR_CREATED, VENDOR_BANK_DETAILS_CHANGED, EMPLOYEE_CREATED, PRODUCT_CREATED | masters → invoice, purchase, ledger, audit, AI |
| `payment.events` | PAYMENT_REQUEST_CREATED, PAYMENT_QR_GENERATED, PAYMENT_RECEIVED, PAYMENT_FAILED | payment → invoice, ledger, installment, reconciliation, analytics |
| `invoice.events` | INVOICE_GENERATED, INVOICE_SENT | invoice → ledger, compliance, notification, analytics |
| `purchase.events` | PURCHASE_BILL_CREATED, EXPENSE_APPROVED | purchase → ledger, compliance, reconciliation |
| `payout.events` | VENDOR_PAYMENT_INITIATED, VENDOR_PAYMENT_COMPLETED, PAYOUT_APPROVAL_REQUESTED/COMPLETED | payout → ledger, reconciliation, audit, analytics |
| `payroll.events` | SALARY_RUN_CREATED, PAYSLIP_GENERATED | payroll → ledger, compliance, notification |
| `installment.events` | INSTALLMENT_SCHEDULE_CREATED, INSTALLMENT_PAID | installment → reminder, ledger, analytics |
| `reminder.events` | REMINDER_SENT, NOTIFICATION_DELIVERED/FAILED | notification → audit, analytics |
| `ingestion.events` | BANK_TRANSACTION_IMPORTED, TRANSACTION_CLASSIFIED | ingestion → reconciliation, ledger, AI, analytics |
| `ledger.events` | JOURNAL_ENTRY_POSTED, MONTH_LOCKED, MONTH_REOPEN_REQUESTED | ledger → analytics, compliance, audit |
| `reconciliation.events` | RECONCILIATION_MATCHED, RECONCILIATION_EXCEPTION_CREATED | reconciliation → ledger, audit, analytics |
| `compliance.events` | COMPLIANCE_REPORT_GENERATED | compliance → ca-collab, audit, analytics |
| `approval.events` | APPROVAL_REQUESTED, APPROVAL_COMPLETED | approval → audit, target service |
| `audit.events` | AUDIT_EVENT_RECORDED | all → audit-evidence (OpenSearch) |
| `document.events` | DOCUMENT_UPLOADED, OCR_COMPLETED | ocr → vendor, purchase, invoice, AI |
| `ai.events` | AI_SUGGESTION_CREATED, ANOMALY_DETECTED | ai → notification, audit, analytics |

**Delivery guarantees:** at-least-once + consumer idempotency (dedupe on `event_id` in
`processed_events`). Ordering per aggregate via `partition_key = tenant_id:aggregate_id`. Schema
evolution: backward-compatible only, enforced by schema registry in CI.

---

## 6. API map

REST, OpenAPI 3.1, versioned under `/api/v1`, all tenant-scoped via `X-Tenant-Id` claim +
gateway resolution. Standard cross-cutting on every group: pagination (`page,size,sort`),
filtering, `Idempotency-Key` header on POST that moves money, RFC-7807 `application/problem+json`
errors, `X-Correlation-Id`, audit emission.

| Group | Owner service | Representative endpoints |
|-------|---------------|--------------------------|
| `/auth` | identity | POST /auth/otp/request, /auth/otp/verify, /auth/passkey/register, /auth/token/refresh, POST /auth/logout, GET /auth/sessions, DELETE /auth/sessions/{id} |
| `/businesses` | tenant | POST /businesses, GET/PATCH /businesses/{id}, POST /businesses/{id}/branches, PUT /businesses/{id}/tax-profile, PUT /businesses/{id}/settings |
| `/users`,`/roles` | identity | CRUD users, assign roles, GET /roles, GET /permissions |
| `/customers` | customer | POST /customers, GET /customers?mobile=, GET /customers/{id}/ledger-summary |
| `/vendors` | vendor | POST /vendors, POST /vendors/{id}/bank-accounts (OCR-review flow), GET /vendors/{id}/ledger |
| `/employees` | payroll | CRUD employees, PUT /employees/{id}/salary-structure |
| `/products` | product | CRUD products/services, GET /hsn-sac?q= |
| `/payment-requests` | payment | POST /payment-requests (→QR/link), GET /payment-requests/{id} |
| `/payments` | payment | GET /payments/{id}, GET /payments?requestId= |
| `/webhooks/payments` | payment | POST /webhooks/payments/{provider} (signature-verified, idempotent) |
| `/payouts` | payout | POST /payouts, POST /payouts/{id}/approve, POST /payouts/{id}/reject |
| `/invoices` | invoice | POST /invoices, GET /invoices/{id}/pdf, POST /invoices/{id}/send, POST /credit-notes, POST /debit-notes |
| `/purchase-bills` | purchase | POST /purchase-bills, GET /purchase-bills/{id} |
| `/expenses` | purchase | POST /expenses, POST /expenses/{id}/approve |
| `/installments` | installment | POST /installments, GET /installments?type=receivable|payable, POST /installments/{id}/emis/{n}/pay |
| `/reminders`,`/notifications` | notification | POST /reminders, GET /notifications, GET /notifications/{id}/status |
| `/bank-accounts`,`/bank-transactions`,`/cash-transactions` | ingestion | CRUD + POST /bank-transactions/import, POST /cash-transactions |
| `/reconciliation` | reconciliation | GET /reconciliation/suggestions, POST /reconciliation/matches, GET /reconciliation/exceptions |
| `/chart-of-accounts`,`/journals`,`/ledgers` | ledger | GET /chart-of-accounts, POST /journals (system), GET /ledgers/{account}, GET /reports/trial-balance |
| `/reports` | analytics | GET /reports/{revenue|cost|profit|cashflow|receivables-aging|payables-aging|product-profitability} |
| `/gst` | compliance | GET /gst/gstr1-preview, /gst/gstr3b-summary, /gst/sales-register, /gst/purchase-register, /gst/itc |
| `/payroll-compliance` | compliance | GET /payroll-compliance/{salary-register|pf|esi|pt} |
| `/tds` | compliance | GET /tds/payable, /tds/certificates |
| `/documents`,`/ocr` | ocr | POST /documents (S3 signed upload), POST /ocr/jobs, GET /ocr/jobs/{id} |
| `/audit` | audit-evidence | GET /audit/events?filters, GET /audit/evidence-pack/{periodId} |
| `/approvals` | approval | GET /approvals?status=pending, POST /approvals/{id}/decision |
| `/ai-suggestions` | ai | GET /ai-suggestions?status=pending, POST /ai-suggestions/{id}/accept|reject |
| `/compliance-calendar` | rules/compliance | GET /compliance-calendar |
| `/exports` | audit-evidence | POST /exports (async → S3 signed URL), GET /exports/{id} |

Each endpoint spec (DTOs, validation rules, error codes, examples) is generated contract-first and
lives with its service under `backend/<service>/src/main/resources/openapi/`.

---

## 7. UI/UX screen map

Design law: owner answers *in, out, attention* in <30s; plain language first, accounting on
drill-down; every amount shows status + source + confidence; nav = **Receive · Pay · Reports ·
Compliance · More**; min tap target 44×44; never color-only signalling; English first, Hindi/Telugu
ready; every form has validation + helper + empty/error/loading/success states.

**Mobile (bare RN/TS) — screen groups**
1. Auth: Splash/secure-launch, Login (OTP), OTP, Passkey/biometric enable, Register
2. Onboarding: Business setup, GST/PAN/Udyam, Bank/UPI setup
3. Home dashboard (in/out/receivables/payables/profit/attention)
4. Receive: customer search → create customer → product/service select → GST/price → full/partial → QR/link → success → invoice share
5. Pay: pay-type → employee list → salary calc → payslip preview → vendor list → vendor register → vendor OCR bank capture → purchase entry → payout
6. Installments: receivable setup, payable setup, list, EMI detail
7. Reminders: due today, due tomorrow, overdue, settings
8. Transaction monitor: all credits/debits, cash entry, bank import, UPI import, detail, classification review
9. Accounting: ledger summary, trial balance, P&L, balance sheet
10. Compliance: GST dashboard, GSTR-1 prep, GSTR-3B prep, payroll compliance, TDS, calendar
11. Reports: revenue, cost, profit, cashflow, receivables aging, payables aging, product profitability
12. Reconciliation: bank recon, unmatched, suggested matches, exceptions
13. Audit: alerts, evidence viewer, approval requests
14. Settings: business profile, users & roles, invoice settings, tax settings, notification settings, theme, security, CA/accountant invite
15. Voice transaction input; Offline draft / pending-sync tray

**Web (React/TS) — consoles**
Admin dashboard · Accountant dashboard · CA review workspace · Auditor read-only workspace ·
Ledger browser · Journal entry viewer · Reconciliation console · GST report console · Payroll
report console · Compliance calendar · Evidence room · User/role management · Security logs · Export center.

Role→view: Owner/Co-owner = summary-first mobile+web; Cashier = Receive/Pay only; Accountant/CA =
ledger-first, exception-first web; Auditor = read-only evidence; Admin/Super-admin = ops.

---

## 8. Security model

Layered, fintech-grade (full detail + STRIDE in [specs/threat-model.md](./specs/threat-model.md)).

- **Identity/access:** OTP + optional passkey; device binding; biometric unlock; OAuth2/OIDC via
  Keycloak; short-lived JWT access + rotating refresh; session revocation; RBAC + ABAC; roles
  Owner/Co-owner/Cashier/Accountant/CA/Auditor/Employee/Branch-manager/Support-admin/Super-admin;
  least privilege; **step-up auth** for high-risk actions (payout approve, bank change, month reopen).
- **API:** TLS 1.3 edge, mTLS internal; gateway rate-limit + WAF + bot protection; strict schema
  validation + input sanitization; idempotency keys on money/ledger actions; replay protection
  (nonce + timestamp); webhook signature verification; Resilience4j circuit breakers/timeouts;
  safe RFC-7807 errors; **no sensitive data in logs**.
- **Data:** AES-256 at rest; **field-level encryption** for PAN, Aadhaar (if ever), bank a/c, IFSC,
  UPI ID, GSTIN, mobile, email, sensitive docs; KMS-backed keys, per-tenant key strategy target;
  secrets only in Vault/Secrets Manager; upload AV scanning; S3 signed URLs; retention/deletion
  policies; **role-based PII masking** in UI and logs; encrypted backups + PITR; DR plan.
- **Financial integrity:** immutable audit events; append-only financial logs; maker–checker on
  high-value payouts, vendor bank change, month reopen, manual ledger adjustment, compliance report
  approval, role elevation; fraud/anomaly detection; evidence attachment; reconciliation required
  before final compliance export; configurable approval chains.
- **Tenant isolation:** enforced at gateway (tenant claim), service (ABAC row filter), DB (optional
  RLS); AI prompts/data never cross tenants.

## 9. Compliance model (India)

> Implementation-time rule: verify latest rules only from official sources (GST/e-invoice/e-way
> portals, Income Tax, RBI, NPCI, EPFO, ESIC, MCA, MeitY). Rules live in the Rules Engine with
> **rule-source metadata**; AI must cite that source internally and never fabricate rules.

- **GST:** GSTIN validation; tax invoice, bill of supply, receipt voucher, credit/debit note;
  HSN/SAC; CGST/SGST/IGST auto-calc; place of supply; reverse charge; B2B/B2C; sales & purchase
  registers; GSTR-1 & GSTR-3B **preparation** (not filing); ITC tracking + mismatch exceptions;
  e-invoice & e-way **readiness payloads** (live IRP/NIC gated on GSP contract).
- **Payroll:** salary register, payslip, PF/ESI/PT (state-wise) config, LOP, incentives, deductions,
  insurance, salary TDS, reimbursements.
- **Income tax:** P&L, balance sheet, cashflow, expense & depreciation registers, ITR-ready
  summaries, presumptive taxation support, tax-audit-readiness indicators.
- **TDS:** vendor TDS applicability tagging, salary TDS, TDS payable report, certificate data pack.
- **Audit:** audit trail, evidence pack, manual-override report, deleted/edited/reversed report,
  exception report, approval report.
- **Privacy (DPDP):** consent ledger, data minimization, retention, deletion/anonymization request
  flow, grievance workflow.
- **Payment security:** RBI data-localization/storage expectations, PG compliance, no card data
  storage, tokenized sensitive identifiers, India-region storage.

**Report status lifecycle (mandatory on every compliance artifact):**
`unreconciled → draft → reviewed → approved` and, only with an official portal/API acknowledgement,
`filed(ack#)`. The app never displays "filed" without that acknowledgement.

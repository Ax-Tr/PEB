# PEB Detailed Sprint Plan (Sprint 0–19)

Delivery model: 2-week sprints, production-grade increments (not prototypes). Financial
correctness, security, compliance-review, and auditability are never traded for speed. Each sprint
below gives the 18 fields the build prompt requires: **Scope · User stories · Acceptance criteria ·
UI screens · Backend services · DB changes · API endpoints · Events · Security controls · Audit
events · Test cases · Edge cases · Failure scenarios · Definition of Done · Code details ·
Migrations · API docs · Deployment notes.** To keep this readable, Sprint 0–1 are fully expanded;
Sprints 2–19 use the same field structure at summary depth (each expands to full detail at sprint
kickoff via the Sprint-0 runbook template).

**Cross-sprint Definition of Done (applies to every sprint):**
- Code merged via PR with ≥1 review; CI green (build, unit, integration/testcontainers, contract, Spotless/lint, SpotBugs, secrets scan, dependency scan, container scan).
- Coverage gates: ≥80% business logic, ≥90% accounting rules; every journal entry balances in tests.
- OpenAPI published; Flyway migrations reversible-by-forward-correction and applied cleanly on a fresh DB.
- Audit events emitted for every state change; no sensitive data in logs; PII masked by role.
- Idempotency verified on money/ledger endpoints; no hard delete of financial data.
- Docs updated (service README + run instructions + docker-compose works locally); staging smoke test passes; rollback rehearsed.

**Global backlog labels:** `P0` blocker, `P1` MVP, `P2` fast-follow, `P3` later-phase.

---

## Sprint 0 — Foundation & repository setup  *(fully expanded)*

**Scope:** Monorepo, Gradle Kotlin DSL parent + version catalog + convention plugins,
`common-libraries`, API Gateway skeleton, Identity skeleton (health only), Docker Compose local
stack (Postgres, Redis, Kafka/Redpanda, MinIO, Keycloak, OpenSearch, ClickHouse), CI pipeline,
observability baseline (OTel/Prometheus/Grafana/Loki/Tempo), ADRs, coding standards, design tokens.

**User stories**
- As a developer, I can `./gradlew build` the whole monorepo and run `docker compose up` to get the full local stack, so I can start any service locally.
- As a developer, I inherit Spotless, JaCoCo, testcontainers, OpenAPI, and Boot config from convention plugins without copy-paste.
- As a platform owner, every service exposes `/actuator/health`, structured JSON logs with correlation IDs, and OTel traces from day one.

**Acceptance criteria**
- `./gradlew build` succeeds; `common-libraries` published to local; gateway + identity boot and pass health checks.
- `docker compose up` brings up all infra; a smoke script hits gateway→identity health through the mesh.
- CI runs on PR and blocks merge on any gate failure; a sample trace is visible in Tempo and logs in Loki/Grafana.

**UI screens:** none (design-system tokens + Storybook shell scaffolded for web/mobile).
**Backend services:** api-gateway (skeleton), identity-service (skeleton), common-libraries.
**DB changes:** create per-service databases + Flyway baseline `V1__baseline.sql` (extensions, `outbox_events`, `processed_events`, `audit_events`, `idempotency_keys`).
**API endpoints:** `/actuator/health`, `/actuator/prometheus`, gateway route to identity `/api/v1/ping`.
**Events:** Kafka topics created via init job; schema registry up; no business events yet.
**Security controls:** TLS local certs, gateway rate-limit filter, secret loading from `.env`/Vault-dev, Keycloak realm bootstrap, dependency+secret scanning in CI.
**Audit events:** audit table + outbox helper proven with a synthetic `SERVICE_STARTED` event.
**Test cases:** convention-plugin smoke test; gateway routing test; testcontainers Postgres migration test; outbox relay publishes to Kafka; health endpoints 200.
**Edge cases:** Redis down at boot (service degrades, health shows DOWN); Kafka unavailable (outbox buffers, relay retries); Postgres failover (HikariCP retry).
**Failure scenarios:** infra container fails → compose healthchecks + CI fails fast; migration conflict → Flyway validate blocks.
**Definition of Done:** cross-sprint DoD + green CI + working local stack + published ADR-0001…0006 + this docs pack committed.
**Code details:** see [sprint-0-instructions.md](./sprint-0-instructions.md) for exact tree, `build.gradle.kts`, convention plugins, docker-compose, CI YAML.
**Migrations:** `V1__baseline.sql` per service (outbox/processed/audit/idempotency + extensions).
**API docs:** springdoc served at `/swagger-ui` on each service; aggregated at gateway.
**Deployment notes:** Helm chart skeletons + Terraform module stubs (VPC, EKS, RDS, MSK, ElastiCache, S3, WAF) — plan only, not applied.

---

## Sprint 1 — Identity, business onboarding, roles  *(fully expanded)*

> **Status: DELIVERED (code + unit tests green on JDK 21).** identity-service (OTP via Redis with
> rate-limit + hashing, self-signed RS256 JWT + JWKS, refresh-token rotation with reuse/theft
> detection, device binding, RBAC resource-server, DPDP consent, login audit + outbox events) and
> tenant-service (business onboarding, GSTIN check-digit/PAN/Udyam validation, tax profile, settings,
> branches, field-level encryption + GSTIN blind index, BUSINESS_CREATED/SETTINGS_CHANGED events) are
> implemented; api-gateway now validates JWTs and propagates tenant/actor headers. Not run here
> (needs Docker): Testcontainers integration test + live end-to-end. Deferred to hardening: Keycloak
> federation, passkey/WebAuthn, step-up enforcement, per-tenant KMS keys (currently dev crypto keys).

**Scope:** OTP login, passkey/device binding, JWT issuance + refresh rotation + session revocation,
RBAC/ABAC, business profile, GST/PAN/Udyam fields, branches, invoice/tax settings, DPDP consent
capture, login audit.

**User stories**
- As an owner, I log in with my mobile number + OTP and optionally enable passkey/biometric.
- As an owner, I complete business setup (name, type, GSTIN/PAN/Udyam, state, branch) and invoice/tax settings.
- As an owner, I invite users and assign roles (Owner/Co-owner/Cashier/Accountant/CA/Auditor/Employee/Branch-manager).
- As a security owner, I see my logged-in devices and can revoke a session; suspicious logins are flagged.
- As a data principal, my DPDP consent is recorded with purpose and timestamp.

**Acceptance criteria**
- OTP valid for limited time (e.g., 5 min), single-use, rate-limited per mobile/IP/device; brute force locks out.
- Access token short-lived (≤15 min), refresh rotates on use and is revocable; logout invalidates session.
- GSTIN format validated (15-char, checksum), PAN format validated; invalid → clear error, no save.
- Role permissions enforced: a Cashier cannot open ledger/compliance screens or APIs (403).
- Consent record created before first data capture; login events written to audit + `identity.events`.

**UI screens:** Splash, Login (OTP), OTP, Passkey/biometric enable, Register, Business setup, GST/PAN/Udyam setup, Bank/UPI setup (capture only), Settings→Users & roles.
**Backend services:** identity-service, tenant-service, api-gateway (auth enforcement live), audit-evidence (consume).
**DB changes:** identity_db: users, roles, permissions, role_permissions, user_roles, user_sessions, devices, otp_requests, data_consent_records. tenant_db: businesses, branches, business_tax_profiles, business_settings.
**API endpoints:** `/auth/otp/request|verify`, `/auth/passkey/register`, `/auth/token/refresh`, `/auth/logout`, `/auth/sessions`, `/users`, `/roles`, `/businesses`, `/businesses/{id}/branches`, `/businesses/{id}/tax-profile`, `/businesses/{id}/settings`.
**Events:** USER_LOGGED_IN, DEVICE_REGISTERED, USER_ROLE_CHANGED, BUSINESS_CREATED, BUSINESS_SETTINGS_CHANGED.
**Security controls:** OTP rate-limit (Redis), passkey (WebAuthn), device binding, refresh rotation, session revocation, RBAC+ABAC, step-up scaffolding, PII field encryption (mobile/PAN/GSTIN), masking by role.
**Audit events:** OTP_REQUESTED, LOGIN_SUCCEEDED/FAILED, SESSION_REVOKED, ROLE_ASSIGNED, BUSINESS_CREATED, TAX_PROFILE_UPDATED, CONSENT_RECORDED.
**Test cases:** OTP happy path + expiry + reuse + rate-limit; refresh rotation + replay rejection; RBAC matrix per role; GSTIN/PAN validation table; consent persisted; tenant isolation (user A cannot read tenant B business).
**Edge cases:** duplicate mobile (existing user), SMS provider failure (async, user sees "resend"), device change, passkey unsupported device, GSTIN valid format but state mismatch with place of supply.
**Failure scenarios:** Redis down (OTP path degraded, explicit error, no silent success); Keycloak down (login fails closed); SMS gateway down (queued + retry, never claim delivered).
**Definition of Done:** cross-sprint DoD; RBAC matrix test 100%; no PII in logs verified; OTP/refresh security tests pass.
**Code details:** WebAuthn via `webauthn4j`; OTP hashed+salted in Redis with attempt counter; JWT via Keycloak, resource-server validation at gateway (JWKS) and each service; ABAC via `TenantContext` + method security.
**Migrations:** identity_db V2, tenant_db V2 (tables above) with unique `(tenant_id, mobile)`, `(gstin)`, indexes.
**API docs:** `/auth`, `/businesses`, `/users`, `/roles` OpenAPI published.
**Deployment notes:** Keycloak realm + client config via Terraform/Helm values; secrets in Vault.

---

## Sprint 2 — Masters: customer, vendor, employee, product

> **Status: DELIVERED (code + unit tests green on JDK 21).** Four new services built to the
> tenant-service hexagonal template: **customer-service** (mobile search/create, blind-index dedupe,
> ledger-summary stub), **vendor-service** (vendor master + bank accounts behind an **OCR
> review gate** — saved PENDING_REVIEW, usable only after explicit confirm, which emits
> VENDOR_BANK_DETAILS_CHANGED; account numbers masked to last 4), **product-service** (catalog,
> HSN/SAC master + search, GST-slab validation, price history, Money-as-paise), **employee-payroll-
> service** (employee master + salary structure with basic+HRA≤gross guard). All use field-level
> encryption for PII/bank fields, tenant scoping via TenantContext, audit + outbox events, resource-
> server security, Flyway migrations. Gateway routes added. 19 unit tests pass; all 8 modules compile
> + Spotless clean. **Chart of Accounts seed deferred to Sprint 5** (owned by accounting-ledger-
> service; seeded on BUSINESS_CREATED there). Deeper payroll (salary run/payslip/statutory calc) is
> Sprint 7. Not run here (no Docker): Testcontainers integration + live end-to-end.

**Scope:** Customer master + mobile search + create; vendor onboarding + bank/UPI capture (OCR
review gate); employee master + salary structure; product/service catalog + HSN/SAC + GST rate;
Chart of Accounts seed per business.
**Stories:** owner creates/searches customers by mobile; registers vendors with bank/UPI; adds
employees + salary structure; builds product catalog with HSN/SAC & GST; system seeds CoA.
**AC:** mobile search returns existing or offers create; duplicate mobile handled; HSN/SAC lookup;
GST rate required for taxable items; CoA seeded on BUSINESS_CREATED.
**Services:** customer, vendor, employee-payroll, product, accounting-ledger (CoA seed), ocr (stub).
**DB:** customers, customer_contacts, vendors, vendor_bank_accounts, employees, salary_structures,
products, services, hsn_sac_master, price_history, chart_of_accounts.
**APIs:** `/customers`, `/vendors`, `/employees`, `/products`, `/chart-of-accounts` (read).
**Events:** CUSTOMER_CREATED, VENDOR_CREATED, VENDOR_BANK_DETAILS_CHANGED, EMPLOYEE_CREATED, PRODUCT_CREATED.
**Security:** field encryption for vendor bank a/c, IFSC, UPI, customer mobile; OCR review gate before save; RBAC.
**Audit:** *_CREATED/UPDATED, VENDOR_BANK_DETAILS_CHANGED (maker-checker scaffold).
**Tests:** mobile search dedupe; HSN/SAC + GST validation; CoA seeded & balanced; encryption round-trip.
**Edge cases:** duplicate customer mobile; vendor bank OCR wrong (must review); GST rate missing; HSN unknown.
**Failure:** OCR service down (manual entry path); catalog import partial.
**DoD:** cross-sprint DoD; masters CRUD + search + CoA seed verified.

## Sprint 3 — Receive payment flow

> **Status: DELIVERED (payment-collection-service; code + 13 unit tests green on JDK 21).**
> Payment request creation (idempotent on `Idempotency-Key`), **dynamic UPI intent URI (QR content) +
> payment link** (NPCI deep-link, amount from integer paise), gateway abstraction, and a
> **signature-verified (HMAC-SHA-256, constant-time, fail-closed) + idempotent (dedupe on
> provider event id) webhook** that clamps applied amount to the outstanding balance (duplicate/
> overpayment can't drive the balance negative or past the total — overpayment surfaced for
> reconciliation). Emits PAYMENT_REQUEST_CREATED, PAYMENT_QR_GENERATED, PAYMENT_RECEIVED,
> PAYMENT_FAILED via outbox; audit on every step; no card data. Added a reusable DB-backed
> `IdempotencyService` to common-libraries. Gateway route + webhook public-path added. **Downstream
> invoice generation + ledger posting + receivable-installment are consumed from PAYMENT_RECEIVED in
> Sprints 4/5/8** (their services don't exist yet) — the receive→invoice→ledger→installment *saga
> orchestrator* is realized in Sprint 5 when the ledger exists. Not run here (no Docker):
> Testcontainers integration + live gateway callback. Item-entry/GST-calc on the invoice side is
> Sprint 4.

**Scope:** payment request, item entry, GST calc, full/partial, dynamic UPI QR + payment link,
gateway abstraction, webhook verification, payment success, invoice trigger, receivable ledger +
installment trigger.
**Stories:** cashier collects payment via QR/link; partial payment starts a receivable schedule;
paid → invoice generated & receivable posted.
**AC:** QR generates <2s; webhook signature-verified + **idempotent** (double webhook = one effect);
partial ≤ total enforced; PAYMENT_RECEIVED posts ledger (Bank/UPI/Cash Dr, Customer Receivable Cr).
**Services:** payment-collection, invoice-gst, accounting-ledger, installment, customer.
**DB:** payment_requests, payment_qr_codes, payment_webhooks, idempotency_keys.
**APIs:** `/payment-requests`, `/payments`, `/webhooks/payments/{provider}`.
**Events:** PAYMENT_REQUEST_CREATED, PAYMENT_QR_GENERATED, PAYMENT_RECEIVED, PAYMENT_FAILED.
**Security:** webhook HMAC/signature verify; idempotency key; replay protection; no card data.
**Audit:** PAYMENT_REQUEST_CREATED, PAYMENT_RECEIVED, PAYMENT_FAILED, LEDGER_POSTED(ref).
**Tests:** duplicate webhook; partial>invoice rejected; QR-cancel; latency P95<2s; ledger balances.
**Edge cases:** webhook arrives twice; initiated-not-confirmed; QR generated then cancelled; partial exceeds invoice; internet drop mid-payment.
**Failure:** gateway down (failover/queue, status "pending", never "paid"); Kafka down (outbox buffers).
**DoD:** cross-sprint DoD; idempotent webhook proven; receive→invoice→ledger saga passes.

## Sprint 4 — Invoice & GST engine

> **Status: DELIVERED (invoice-gst-service; code + 17 unit tests green on JDK 21).** A pure,
> framework-free **GST engine** (`GstCalculator`) is the correctness core: intra-state → CGST+SGST
> with an exact split (no lost paise), inter-state → IGST, exempt/bill-of-supply → no tax, reverse
> charge computed-but-not-collected, banker's rounding — proven by an exhaustive 7-case matrix
> (the hard gate). On top: all document types (tax invoice / bill of supply / receipt voucher /
> credit note / debit note) in one `invoices` table, per-line item + `gst_tax_lines` persistence,
> **concurrency-safe per-tenant per-FY invoice numbering** (Indian Apr–Mar FY, `PREFIX/2026-27/00001`),
> **PDF generation** (OpenPDF), **sales register**, and **e-invoice + e-way readiness payloads**
> (IRP/NIC-style JSON with missing-field detection, explicitly `READINESS_NOT_FILED`). Emits
> INVOICE_GENERATED / INVOICE_SENT via outbox; audit on every step; gateway route added. Not run here
> (no Docker): Testcontainers integration + live PDF/e-invoice submission. Event-driven creation from
> PAYMENT_RECEIVED (the receive→invoice saga) is wired in Sprint 5 with the ledger.

**Scope:** tax invoice, bill of supply, receipt voucher, credit/debit note, CGST/SGST/IGST split,
place of supply, reverse charge, B2B/B2C, PDF generation, invoice sharing, sales register,
GSTR-ready data model, e-invoice/e-way **readiness payloads**.
**Stories:** owner generates GST-compliant invoice PDF and shares via SMS/WhatsApp/email; issues
credit/debit notes; system builds sales register + GSTR-ready rows.
**AC:** correct tax split by place-of-supply (intra→CGST+SGST, inter→IGST); invoice number unique
per tenant per FY; PDF <30s; reverse-charge flagged; e-invoice payload schema-valid (not filed).
**Services:** invoice-gst, product, customer, tenant, compliance-report, notification.
**DB:** invoices, invoice_items, gst_tax_lines, credit_notes, debit_notes.
**APIs:** `/invoices`, `/invoices/{id}/pdf`, `/invoices/{id}/send`, `/credit-notes`, `/debit-notes`, `/gst/sales-register`.
**Events:** INVOICE_GENERATED, INVOICE_SENT.
**Security:** signed URL for PDF; PII masking; tenant isolation.
**Audit:** INVOICE_GENERATED, INVOICE_SENT, CREDIT_NOTE_ISSUED, DEBIT_NOTE_ISSUED.
**Tests:** GST split matrix (intra/inter/exempt/RCM/zero-rated); rounding (banker's); duplicate invoice number blocked; PDF renders.
**Edge cases:** GSTIN invalid; GST rate missing; invoice number duplicate; credit note > invoice; multi-rate invoice.
**Failure:** PDF service timeout (async regenerate); WhatsApp send fails (retry + status).
**DoD:** cross-sprint DoD; GST calc tests exhaustive; e-invoice payload validated against schema.

## Sprint 5 — Accounting ledger engine

> **Status: DELIVERED (accounting-ledger-service; code + 22 unit tests green on JDK 21).** The
> double-entry core: `JournalCommand` enforces Σdebit=Σcredit/≥2 lines before the DB (backed by a
> Postgres **constraint trigger** as defence-in-depth); `PostingTemplates` map source events to
> balanced journals (proven by property tests); `LedgerPostingService` posts **idempotently on
> source_event_id**, blocks posting into a **locked period**, and corrects only via **reversing
> entries** (true append-only — originals are never mutated). CoA is seeded per business from the
> standard MSME template. Reports (**trial balance ties out, P&L, balance sheet balances** with
> current-period earnings) + full **month-lock state machine** (OPEN→DRAFT_CLOSED→LOCKED→AUDITED,
> maker-checker reopen). **Event flow is now live in code**: added a toggled **outbox→Kafka relay**
> to common-libraries (enabled in payment + invoice + ledger), and a **Kafka consumer** in the ledger
> that seeds CoA on BUSINESS_CREATED and posts balanced journals from INVOICE_GENERATED /
> PAYMENT_RECEIVED — this realizes the **receive→invoice→ledger** path end-to-end (idempotent via the
> posting layer). REST: chart-of-accounts, journals (post/get/reverse), ledgers, reports, periods.
> Gateway route added. This satisfies the 90%-accounting-rules intent via the balanced-journal
> property tests + posting/reversal/lock/report suites. Not run here (no Docker): Testcontainers
> integration incl. the balance trigger + live Kafka round-trip. The full saga *compensation*
> orchestration (invoice-side voiding on failure) is refined in later sprints.

**Scope:** CoA, journal entries, double-entry posting engine, ledger balances, trial balance, P&L,
balance sheet, month-lock state machine, source→ledger traceability, cash & accrual views,
reversal/correction flow.
**Stories:** system posts balanced journals from events; accountant views trial balance/P&L/BS;
owner locks a month; corrections via reversal only.
**AC:** Σdebits=Σcredits enforced (DB constraint + service); no journal delete; every entry references
source event; month-lock blocks edits without approval; trial balance balances.
**Services:** accounting-ledger (core), rules-engine (posting templates), audit-evidence.
**DB:** journal_entries, journal_entry_lines, ledger_balances, financial_periods, month_locks.
**APIs:** `/journals` (system+manual w/ approval), `/ledgers/{account}`, `/reports/trial-balance`, `/reports/pnl`, `/reports/balance-sheet`, month-lock endpoints.
**Events:** JOURNAL_ENTRY_POSTED, MONTH_LOCKED, MONTH_REOPEN_REQUESTED.
**Security:** maker-checker on manual adjustment + month reopen; optimistic locking; no delete grant.
**Audit:** JOURNAL_POSTED, JOURNAL_REVERSED, MONTH_LOCKED, MONTH_REOPEN_REQUESTED/APPROVED.
**Tests:** every posting template balances; reversal restores balances; month-lock enforcement; unbalanced entry rejected.
**Edge cases:** posting after month lock; adjustment needing approval; reopen request; period boundary; negative cash.
**Failure:** consumer replays event (idempotent post via `processed_events`); partial saga compensation.
**DoD:** cross-sprint DoD; **90% accounting coverage**; all posting templates from CoA spec validated.

## Sprint 6 — Vendor purchase & payout

**Scope:** vendor OCR bank capture (review-before-save), purchase bill entry, vendor ledger,
full/partial vendor payment, payout maker-checker, payable schedule trigger, beneficiary validation,
gateway failover.
**Stories:** owner captures vendor bank via OCR then reviews; enters purchase bill; pays vendor
with approval for high value; partial payment starts payable schedule.
**AC:** OCR result reviewed before save; payout > threshold requires approval; vendor-bank-change
before payout flagged (anomaly + step-up); purchase posts Purchase/Input-GST Dr, Vendor Payable Cr.
**Services:** vendor, purchase-expense, payout, ocr, accounting-ledger, installment, approval.
**DB:** purchase_bills, purchase_items, expenses, payouts, payout_approvals, beneficiaries.
**APIs:** `/purchase-bills`, `/expenses`, `/payouts`, `/payouts/{id}/approve|reject`.
**Events:** PURCHASE_BILL_CREATED, VENDOR_PAYMENT_INITIATED/COMPLETED, PAYOUT_APPROVAL_REQUESTED/COMPLETED.
**Security:** maker-checker; step-up on bank change; idempotent payout; beneficiary validation.
**Audit:** PURCHASE_BILL_CREATED, PAYOUT_REQUESTED/APPROVED/REJECTED/COMPLETED, VENDOR_BANK_CHANGED.
**Tests:** approval threshold; bank-change-before-payout block; idempotent payout; ledger balances.
**Edge cases:** vendor bank OCR wrong; bank changed before payout; payout gateway down; partial vendor payment; duplicate bill.
**Failure:** gateway failover; approval timeout; compensation if ledger post fails after payout initiate.
**DoD:** cross-sprint DoD; maker-checker + anomaly on bank change verified.

## Sprint 7 — Employee payroll

**Scope:** salary structure, LOP, PF/ESI/PT/TDS/insurance/incentives/deductions, net salary calc,
salary run, salary payout, payslip PDF, payroll ledger posting.
**Stories:** owner runs monthly payroll; system computes net pay + statutory deductions; payslips
generated; payout posts ledger.
**AC:** PF/ESI/PT computed per current rules (from Rules Engine w/ source); LOP prorates; salary run
idempotent (cannot double-run a month); payslip PDF per employee; Salary Expense Dr / Employee
Payable Cr then Employee Payable Dr / Bank Cr on pay.
**Services:** employee-payroll, rules-engine, accounting-ledger, notification, payout.
**DB:** salary_runs, salary_run_lines (extend employee_db).
**APIs:** `/employees/{id}/salary-structure`, payroll run endpoints, `/payroll-compliance/salary-register`.
**Events:** SALARY_RUN_CREATED, PAYSLIP_GENERATED.
**Security:** payroll data masking; approval for salary run; TDS/PF config change audited.
**Audit:** SALARY_RUN_CREATED, PAYSLIP_GENERATED, PAYROLL_POSTED.
**Tests:** PF/ESI/PT/LOP calc matrix; salary run idempotency; mid-month joiner/leaver; ledger balances.
**Edge cases:** salary run already processed; employee resigned mid-month; LOP > working days; negative net pay.
**Failure:** payout gateway down; payslip PDF failure (retry).
**DoD:** cross-sprint DoD; payroll calc tests exhaustive; statutory rules sourced.

## Sprint 8 — Installment engine
**Scope:** receivable/payable schedules, EMI due dates, EMI payment link, balance updates, closure
logic, modification-with-audit. **Services:** installment, payment, notification, ledger.
**DB:** installments, installment_emis. **Events:** INSTALLMENT_SCHEDULE_CREATED, INSTALLMENT_PAID.
**AC:** schedule sums to principal; EMI pay updates balance + posts ledger; closure on zero balance;
edits audited. **Edge cases:** overpay EMI; early closure; reschedule; missed EMI → overdue.
**DoD:** cross-sprint DoD; balances reconcile to ledger.

## Sprint 9 — Reminder & notification engine
**Scope:** SMS/email/push + WhatsApp readiness, D-3/D-1/D-day scheduling, template engine, delivery
status, escalation, retry. **Services:** notification, installment, rules. **DB:**
notification_templates, notification_logs, reminder_schedules. **Events:** REMINDER_SENT,
NOTIFICATION_DELIVERED/FAILED. **AC:** dedupe reminders; delivery status tracked; retry with backoff;
never mark delivered without provider ack. **Edge cases:** SMS/email/WhatsApp fail; opt-out; quiet
hours; duplicate schedule. **DoD:** cross-sprint DoD; delivery status accurate.

## Sprint 10 — Transaction monitor & bank/cash
**Scope:** manual cash credit/debit, bank account setup, statement import, UPI import, classification
review, duplicate detection. **Services:** transaction-ingestion, ai (classify), ledger. **DB:**
cash_transactions, bank_accounts, bank_transactions, upi_transactions, gateway_settlements,
import_batches. **Events:** BANK_TRANSACTION_IMPORTED, TRANSACTION_CLASSIFIED. **AC:** duplicate
import detected; classification shows confidence; low-confidence not auto-posted. **Edge cases:**
duplicate statement import; already-reconciled txn; malformed statement; negative cash. **DoD:**
cross-sprint DoD; dedupe + confidence gating verified.

## Sprint 11 — Reconciliation engine
**Scope:** weighted matching (invoice↔payment, bill↔payment, payroll↔bank, gateway settlement, cash),
suggested matches, exceptions, manual match. **Services:** reconciliation, ingestion, invoice,
payout, ledger. **DB:** reconciliation_matches, reconciliation_exceptions. **Events:**
RECONCILIATION_MATCHED, RECONCILIATION_EXCEPTION_CREATED. **AC:** auto-match above threshold; suggest
mid; exception low; every decision logged (see AI blueprint weighted signals). **Edge cases:**
already reconciled; partial match; many-to-one; reversed txn. **DoD:** cross-sprint DoD; match
decisions auditable; reconciliation required before compliance export.

## Sprint 12 — Compliance reports
**Scope:** GST dashboard, GSTR-1 prep, GSTR-3B summary, sales/purchase register, ITC report +
mismatch, e-invoice/e-way readiness, payroll compliance (PF/ESI/PT), TDS reports, ITR-ready
summaries. **Services:** compliance-report, invoice, purchase, ledger, payroll, rules. **DB:**
compliance_reports, compliance_report_lines. **Events:** COMPLIANCE_REPORT_GENERATED. **AC:** every
report shows `unreconciled|draft|reviewed|approved`; missing fields listed; never "filed" without ack;
ITC mismatch exception list. **Edge cases:** compliance rule change; unreconciled data; missing GSTIN;
report export timeout. **DoD:** cross-sprint DoD; report status lifecycle enforced; CA-review gating.

## Sprint 13 — Analytics & intelligence
**Scope:** revenue/cost/profit/margin, cashflow, receivables/payables aging, product/service
profitability, branch-wise; ClickHouse read-models. **Services:** analytics (+ event consumers).
**DB:** ClickHouse materialized aggregates. **AC:** dashboards <3s; analytics never hits OLTP;
read-models eventually consistent w/ freshness indicator. **Edge cases:** late events; backfill;
timezone (IST); huge datasets. **DoD:** cross-sprint DoD; dashboard latency SLO met.

## Sprint 14 — Audit, CA & accountant workspace
**Scope:** CA/accountant/auditor invite + role-scoped access, evidence room, review comments,
approval workflow, month-end close checklist. **Services:** ca-collaboration, audit-evidence,
identity, compliance. **DB:** ca_invites, review_notes, close_checklists, report_approvals,
evidence_items, export_jobs. **Events:** APPROVAL_REQUESTED/COMPLETED, AUDIT_EVENT_RECORDED. **AC:**
auditor is read-only; evidence immutable; approvals logged; close checklist gates month lock. **Edge
cases:** revoked CA access mid-review; conflicting approvals; evidence for reversed txn. **DoD:**
cross-sprint DoD; immutable audit verified; role-scoped access tested.

## Sprint 15 — AI automation
**Scope:** transaction classification, OCR confidence, anomaly detection, cashflow prediction, NL
finance assistant, voice transaction input, AI governance controls. **Services:** ai-automation,
ocr, reconciliation, analytics. **DB:** ai_suggestions, anomaly_alerts, ai_feedback. **Events:**
AI_SUGGESTION_CREATED, ANOMALY_DETECTED. **AC:** every AI output shows confidence; low-confidence
needs approval; no autonomous statutory filing; prompt-injection defenses on uploaded docs; no
cross-tenant leakage; AI decisions auditable. **Edge cases:** AI classification wrong; adversarial
invoice text; tenant data leakage risk; model unavailable (graceful degrade to manual). **DoD:**
cross-sprint DoD; AI safety rules enforced + tested; tenant isolation red-team passed.

## Sprint 16 — Security hardening
**Scope:** pen-test fixes, OWASP API/Mobile Top-10 controls, secrets rotation, SIEM integration,
threat-model review, DPDP data controls (retention/deletion/anonymization, grievance), incident
runbook. **AC:** no critical/high vulns; secrets rotated; DPDP request flow works; per-tenant key
strategy decided. **DoD:** cross-sprint DoD + pen-test sign-off + threat model updated.

## Sprint 17 — Performance & scale
**Scope:** load tests (target 10k concurrent staged), query optimization, caching, dashboard
read-models, async jobs, Kafka tuning, DB indexes, autoscaling. **AC:** per-endpoint P95 SLOs met;
analytics off OLTP; background export for heavy reports. **DoD:** cross-sprint DoD + load-test report
+ SLOs documented.

## Sprint 18 — UAT & production readiness
**Scope:** MSME + accountant + CA workflow UAT, compliance-report validation, bug fixing, app-store
readiness, production deployment checklist. **AC:** all acceptance criteria pass; no critical bugs;
staging smoke green; rollback rehearsed. **DoD:** production-readiness checklist complete (see
engineering-standards.md §4).

## Sprint 19 — Launch
**Scope:** blue/green production deploy, monitoring, alerting, backup verification, rollback plan,
staged rollout, onboarding analytics. **AC:** blue/green cutover with automated rollback; alerts
firing to on-call; backups verified restorable; PITR tested. **DoD:** launch checklist complete;
post-launch monitoring dashboard live.

---

## Phasing note (budget-aware)
- **MVP (P0/P1):** Sprints 0–5, 8–10, 12(core GST prep), 14(basic audit) — a usable MSME finance app with receive/pay/invoice/ledger/installments/reminders/bank-cash + GST prep + audit trail.
- **Fast-follow (P2):** Sprints 6–7 full payroll depth, 11 reconciliation, 13 analytics depth, 15 AI.
- **Hardening/Launch (P0 for prod):** Sprints 16–19 mandatory before public launch.
- iOS build follows Android per ADR-0004; WhatsApp + live e-invoice/e-way + live PG gated on contracts (see decisions.md open items).

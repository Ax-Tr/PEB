# Spec: STRIDE Threat Model & Control Mapping

Scope: PEB backend microservices, mobile, web, payment/webhook surfaces, AI, data stores.
Reviewed each hardening sprint (16) and on any new external integration.

## 1. Assets
Payment credentials/tokens, bank/UPI/IFSC, PAN/GSTIN/Aadhaar(if ever), mobile/email, invoices,
payslips, ledgers, tax reports, audit trail, business financial position, auth tokens, KMS keys.

## 2. Trust boundaries
Internet ↔ Gateway · Gateway ↔ services (mTLS) · services ↔ data stores · services ↔ external
(PG, SMS/WhatsApp, IRP/NIC, banks, model providers) · mobile/web ↔ Gateway · tenant ↔ tenant.

## 3. STRIDE → controls
| Threat | Example | Controls |
|--------|---------|----------|
| **Spoofing** | stolen OTP, forged webhook, token replay | OTP rate-limit + single-use; passkey/device binding; webhook signature verify; short JWT + refresh rotation; mTLS internal; step-up auth |
| **Tampering** | alter ledger, replay request, modify invoice | append-only financial tables (no UPDATE/DELETE grant); balanced-entry DB constraint; request signing + nonce; optimistic locking; object integrity hashes on evidence |
| **Repudiation** | "I didn't approve that payout" | immutable audit events; maker-checker records; correlation IDs; signed approvals; evidence pack |
| **Information disclosure** | cross-tenant read, PII in logs, leaked S3 | tenant isolation (gateway+ABAC+optional RLS); field-level encryption; PII masking in UI/logs; signed URLs; no secrets in logs; AI tenant isolation |
| **Denial of service** | OTP flooding, webhook storm, report bombs | gateway rate-limit + WAF + bot protection; bulkheads; async heavy exports; autoscaling; circuit breakers |
| **Elevation of privilege** | cashier hits ledger API, role tamper | RBAC+ABAC enforced at gateway+service; least privilege; role change is maker-checker + audited; method-level security |

## 4. Payment/webhook specific
- Verify signature before parsing; idempotent on provider event id; reject stale timestamps (replay);
  never trust amount/status from client — reconcile against gateway settlement; no card data stored;
  sensitive identifiers tokenized; India-region storage (RBI).

## 5. AI/document specific
- Prompt-injection defenses on uploaded invoices/bills (treat extracted text as untrusted; never let
  it trigger tool/actions); confidence required; low-confidence never auto-posts; per-tenant context
  isolation; AI decisions logged + auditable; no autonomous statutory filing.

## 6. Mobile specific (OWASP Mobile Top-10)
- Tokens in Keychain/Keystore; biometric unlock; cert pinning; jailbreak/root signal; no secrets in
  bundle; encrypted offline store; screenshot/clipboard protection on sensitive screens.

## 7. Residual risks / open items
- Per-tenant KMS key cost (decide Sprint 16); WhatsApp/IRP/PG vendor security posture (contract due
  diligence); model-provider data handling (DPA + no-training clause required).

# UAT Scripts (Sprint 18)

Scripted user-acceptance sessions for the three PEB personas plus compliance validation. Each step
lists the action, the endpoint it exercises, and the expected result / acceptance check. Run against
staging via the gateway (`/api/v1/...`) with a real token per persona role. Sign-off requires every
"Expected" to hold and every non-negotiable (see `acceptance-traceability.md`) to be observed.

Legend: **AC** = acceptance check that must pass for sign-off.

## Persona A — MSME business owner (role OWNER)

1. **Onboard business** → `POST /api/v1/businesses`
   - AC: business + default chart of accounts created; owner can log in via OTP (`/api/v1/auth/otp/*`).
2. **Add a customer** → `POST /api/v1/customers`
   - AC: customer stored; PAN/contact returned masked in list views.
3. **Create & send an invoice** → `POST /api/v1/invoices`, `POST /api/v1/invoices/{id}/send`
   - AC: GST computed correctly (intra/inter split); invoice number sequential; `INVOICE_GENERATED`
     emitted; status is DRAFT→ISSUED→SENT (a send is SENT only after the channel accepts).
4. **Collect a payment** → `POST /api/v1/payment-requests`, then simulate provider webhook
   `POST /api/v1/webhooks/payments/...`
   - AC: webhook **signature-verified**; **double-delivery is idempotent** (send twice → one payment);
     `PAYMENT_RECEIVED` emitted; cash inflow visible in analytics.
5. **View dashboards** → `GET /api/v1/analytics/pnl|cashflow|receivables-aging`
   - AC: figures reconcile with the invoice/payment; dashboard returns < 3 s; `GET /analytics/freshness`
     shows FRESH.
6. **Ingest a bank statement line** → `POST /api/v1/bank-transactions`
   - AC: transaction stored, classified with a **confidence**, and **NOT auto-posted** to the ledger.

## Persona B — Accountant (role ACCOUNTANT)

1. **Review AI suggestions** → `GET /api/v1/ai/suggestions?status=PROPOSED`, `POST /suggestions/{id}/accept`
   - AC: every suggestion shows a confidence; low-confidence items require this explicit accept;
     statutory suggestions are never auto-applied.
2. **Reconcile** → `GET /api/v1/reconciliation/...`, confirm/reject matches
   - AC: reversal-only corrections; no hard delete; matches audited.
3. **Post a manual journal / adjustment** → ledger endpoints
   - AC: Σdebits = Σcredits enforced (unbalanced rejected); manual adjustment needs maker-checker;
     month-locked period rejects edits without approval.
4. **Generate a compliance report** → `POST /api/v1/compliance/reports/generate`
   - AC: report is DRAFT; shows `displayState = UNRECONCILED` until data is reconciled; missing-field
     flags listed.
5. **Mark reconciled & submit for review** → `POST /compliance/reports/{id}/reconciled`,
   `/review`
   - AC: cannot APPROVE until reconciled; review by ACCOUNTANT/CA only.

## Persona C — Chartered Accountant / auditor (role CA; auditor = read-only collaborator)

1. **Accept CA invitation** → `POST /api/v1/collaboration/invites/{id}/accept`
   - AC: access granted only after accept; revoking the invite mid-review immediately blocks further
     contributions.
2. **Add review notes** → `POST /api/v1/collaboration/notes`
   - AC: an **auditor** (read-only collaborator) is blocked from writing; a CA can add notes; notes are
     append-only.
3. **Approve a report (checker)** → `POST /api/v1/collaboration/approvals/{id}/decide`
   - AC: **maker-checker** — the approver must differ from the requester; a second decision conflicts.
4. **Complete the month-end close checklist** → `POST /api/v1/collaboration/checklists/items/{id}`
   - AC: `canLockMonth` becomes true only when every mandatory item is done; drives the ledger month lock.
5. **Record filing acknowledgement** → `POST /api/v1/compliance/reports/{id}/filing`
   - AC: report reaches FILED **only** with a non-blank official acknowledgement; recording the ack
     does not itself claim the return was filed with the portal.

## Compliance-report validation

- **GSTR-3B net payable** = output GST − ITC (clamped at 0); flags "ITC exceeds output".
- **Sales/purchase registers** split B2B/B2C; totals reconcile with invoices/bills.
- **Payroll compliance** sums PF/ESI/PT + salary TDS; **TDS summary** always flags the vendor-TDS gap.
- **Every report** shows status (unreconciled/draft/reviewed/approved/filed) and never claims "filed"
  without an acknowledgement.

## Data-rights (DPDP) validation

- Submit an ERASURE request → verify identity → `POST /privacy/requests/{id}/erasure-plan`.
- AC: financial/tax/KYC categories return `RETAIN_LEGAL_HOLD` (not deleted); `fullErasurePossible=false`
  with an honest summary; `DATA_ERASURE_REQUESTED` emitted; SLA due date tracked.

## Sign-off

A persona session passes only when all its ACs hold with **no critical bug**. Record sign-off from a
real MSME owner, a practising accountant, and a CA (DoD requirement).

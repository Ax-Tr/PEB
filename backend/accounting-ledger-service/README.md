# accounting-ledger-service

The platform's double-entry general ledger (Java 21, Spring Boot 3.3). Port `8089`, owns `ledger_db`.

## What it does

- **Chart of accounts** — seeds an MSME-simplified CoA per tenant (idempotent), on the
  `BUSINESS_CREATED` event or via `POST /api/v1/chart-of-accounts/seed`.
- **Double-entry posting** — every journal is balanced (Σdebit = Σcredit), idempotent on its source
  event, and blocked when the target period is locked. Posted from:
  - the REST API (`POST /api/v1/journals`), and
  - upstream events consumed from Kafka (`INVOICE_GENERATED` → customer-invoice journal,
    `PAYMENT_RECEIVED` → customer-payment journal).
- **Append-only + reversal-only** — journals are never mutated or deleted. Corrections are new
  reversing entries (`POST /api/v1/journals/{id}/reverse`).
- **Reports** — trial balance, profit & loss, and balance sheet, derived from `ledger_balances`.
- **Month-lock** — the `OPEN → DRAFT_CLOSED → LOCKED → AUDITED` lifecycle with a maker-checker
  reopen. Locking blocks further posting into the period.

## Eventing

Consumes `tenant.events`, `invoice.events`, `payment.events` (consumer group `accounting-ledger`).
Emits `JOURNAL_ENTRY_POSTED`, `MONTH_LOCKED`, and `MONTH_REOPEN_REQUESTED` via the transactional
outbox relay.

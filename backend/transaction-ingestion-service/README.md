# transaction-ingestion-service

Captures cash/bank/UPI/settlement transactions for a tenant and prepares them for reconciliation.

Port `8094`, database `ingestion_db`.

## Capabilities

- **Manual entry** — record individual cash (`/api/v1/cash-transactions`) and bank
  (`/api/v1/bank-transactions`) transactions. Cash entries are always `MANUAL_CASH`; direct bank
  entries are `MANUAL_BANK`.
- **Statement / UPI / settlement import** — bulk import feed rows via
  `POST /api/v1/bank-transactions/import` using an import source (`BANK_IMPORT`, `UPI_IMPORT`,
  `GATEWAY_SETTLEMENT`). Import is **idempotent**: rows are deduplicated by a per-tenant dedupe key,
  so re-importing the same statement adds no duplicates (the result reports imported vs. duplicate
  counts).
- **Rule-based classification + review** — each transaction is auto-classified with a confidence
  score. High-confidence rows (>= `peb.ingestion.auto-confirm-confidence`, default `0.90`) are
  confirmed automatically; the rest land in the review queue
  (`GET /api/v1/bank-transactions/review-queue`) for a human to confirm/override via
  `POST /api/v1/bank-transactions/{id}/review`.

## Boundary

Imported rows are the **external source of truth for reconciliation** — they are *not* posted to
the accounting ledger by this service. Downstream services consume the emitted events
(`BANK_TRANSACTION_IMPORTED`, `TRANSACTION_CLASSIFIED`).

## Security

Resource server validating identity-service JWTs. All API endpoints require authentication; tenant
scoping is enforced via `TenantContext`. Actuator health/info/prometheus and Swagger UI are public.

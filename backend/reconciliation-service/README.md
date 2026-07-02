# reconciliation-service

Matches externally-imported bank/settlement rows against the business's own internal records.

## What it does

- **Weighted match engine** — scores candidate pairs on amount, reference, date, counterparty name,
  and narration similarity.
- **Decision bands** — a score maps to `AUTO` (recorded and reconciled immediately), `SUGGESTED`
  (queued for a human to confirm/reject), or `EXCEPTION` (no confident match — flagged for review).
- **Two sides**
  - *External* — imported bank transactions (`ingestion.events` → `BANK_TRANSACTION_IMPORTED`).
    External rows are the **source of truth**; each is matched against internal candidates.
  - *Internal* — the business's own records: payments (`payment.events`), invoices
    (`invoice.events`), and payouts (`payout.events`).
- **Actions** — `POST /run` executes matching; `GET /suggestions` and `GET /exceptions` list open
  work; `confirm` / `reject` resolve suggestions; `matches/manual` pairs items by hand.

## API

Base path `/api/v1/reconciliation`. All endpoints require a valid JWT (resource server). Port 8095,
database `reconciliation_db`.

## Notes

Upstream events currently omit the transaction date, so ingest-time (Asia/Kolkata) is used as the
item date. A future event-schema enrichment should carry the real date for more accurate matching.

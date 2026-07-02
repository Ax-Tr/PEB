# installment-service

Receivable/payable **EMI schedules** with due dates, per-EMI payment tracking, auto-closure, and
audited rescheduling. Owns `installment_db`. Emits `INSTALLMENT_SCHEDULE_CREATED` / `INSTALLMENT_PAID`
(consumed by the reminder service for D-3/D-1/D-day nudges and by analytics for aging).

## Design
- **Exact EMI split** (`EmiScheduleGenerator`): the principal is split into N installments that sum
  to the principal to the paise — remainder paise go one-per-EMI to the earliest installments.
- **Balance tracking + closure**: each payment reduces the outstanding balance; the schedule closes
  automatically at zero.
- **Modification is audited**: rescheduling the remaining balance preserves paid EMIs and is rejected
  if an EMI is partially paid (settle it first).
- **No ledger posting here** — actual cash movement is booked by payment-collection (receivable) and
  payout (payable); this service tracks the schedule only, avoiding double-entry duplication.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :installment-service:bootRun        # :8092
```

## Key endpoints
- `POST /api/v1/installments` — create a schedule (RECEIVABLE|PAYABLE)
- `GET  /api/v1/installments?type=RECEIVABLE` · `GET /api/v1/installments/{id}`
- `POST /api/v1/installments/{id}/pay` — pay a specific EMI
- `POST /api/v1/installments/{id}/modify` — reschedule the balance (audited)
- `POST /api/v1/installments/{id}/cancel`

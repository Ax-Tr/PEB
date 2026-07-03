# commitment-service

Payment commitment tracking for PayWithEase: customer/vendor promises, due dates, partial payment
tracking, rescheduling, broken-promise detection, and audited closure.

## Design
- **Promise lifecycle**: `PROMISED -> PARTIALLY_PAID -> PAID`, plus `BROKEN`, `RESCHEDULED`, and
  `CANCELLED`.
- **No hard-delete**: commitments are stateful financial-adjacent records with immutable audit/event
  history.
- **Idempotent mutations**: create and payment recording accept `Idempotency-Key`.
- **Source linking**: commitments can link to invoice, payment request, installment, voice draft, or
  manual source metadata.
- **Reminder-ready**: `COMMITMENT_CREATED`, `COMMITMENT_RESCHEDULED`, and `COMMITMENT_BROKEN` events
  carry due dates for notification automation.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :commitment-service:bootRun        # :8102
```

## Key endpoints
- `POST /api/v1/commitments`
- `GET  /api/v1/commitments?status=&counterpartyType=`
- `GET  /api/v1/commitments/due-soon?days=7`
- `GET  /api/v1/commitments/overdue`
- `GET  /api/v1/commitments/{id}`
- `POST /api/v1/commitments/{id}/record-payment`
- `POST /api/v1/commitments/{id}/reschedule`
- `POST /api/v1/commitments/{id}/cancel`

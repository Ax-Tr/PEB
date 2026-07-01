# payment-collection-service

Receive-flow money-in service — payment request creation, **dynamic UPI QR / payment link**
generation, gateway abstraction, **signature-verified idempotent webhooks**, partial-payment
tracking, and settlement status. Owns `payment_db`. Emits `PAYMENT_REQUEST_CREATED`,
`PAYMENT_QR_GENERATED`, `PAYMENT_RECEIVED`, `PAYMENT_FAILED` (consumed by invoice/ledger/installment/
reconciliation in their sprints).

## Non-negotiable properties (product rules)
- **Webhook signature verified before parsing** (HMAC-SHA-256, constant-time, fail-closed); optional
  timestamp freshness check for replay defence.
- **Idempotent** on `(provider, providerEventId)` — a redelivered webhook is a no-op. Request
  creation is idempotent on the `Idempotency-Key` header.
- Applied amount is **clamped to the outstanding balance**, so a duplicate/over-amount settlement can
  never drive the balance negative or past the requested total (overpayment is surfaced for
  reconciliation, not credited).
- No card data stored; amounts are integer paise.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :payment-collection-service:bootRun        # :8087
```

## Key endpoints
- `POST /api/v1/payment-requests` (header `Idempotency-Key`) → returns `upiUri` (QR content) + `paymentLink`
- `GET  /api/v1/payment-requests/{id}` → status + amounts
- `POST /api/v1/webhooks/payments/{provider}` (public, `X-Webhook-Signature`, optional `X-Webhook-Timestamp`)

## Config
| Key | Default | Purpose |
|-----|---------|---------|
| `peb.payments.upi.default-vpa` | merchant@upi | payee VPA when not supplied per-request |
| `peb.payments.webhook-secrets.<provider>` | dev-webhook-secret (UPI) | HMAC secret (Vault in prod) |
| `peb.payments.request-expiry-minutes` | 60 | request validity |
| `IDENTITY_JWKS_URI` | http://localhost:8081/oauth2/jwks | token validation |

## Webhook payload (canonical, normalized by the gateway adapter before signing)
```json
{ "eventId": "...", "reference": "PEB01J...", "amountMinor": 150000,
  "status": "SUCCESS", "providerPaymentId": "pay_abc", "timestamp": 1751328000 }
```

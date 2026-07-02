# payout-service

Money-out service — vendor/employee payouts with fintech-grade controls. Owns `payout_db`. Emits
`PAYOUT_APPROVAL_REQUESTED/COMPLETED` and `VENDOR_PAYMENT_INITIATED/COMPLETED`; the accounting-ledger
consumer posts the disbursement journal (Vendor Payable Dr / Bank Cr) from `VENDOR_PAYMENT_COMPLETED`.

## Controls (product rules)
- **Beneficiary validation** — payouts only to a registered, active beneficiary (bank details stored as a blind index, never plaintext).
- **Risk-based maker-checker** — high-value (over threshold) or recently-changed-beneficiary payouts go to `PENDING_APPROVAL`; the **maker can never approve their own payout**.
- **Step-up authentication** — a high-risk payout can't even be created without `X-Step-Up-Verified: true` (simulating completed step-up).
- **Idempotent** creation on the `Idempotency-Key` header.
- **Gateway failover** — the router tries ordered providers until one disburses; attempts are recorded.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :payout-service:bootRun        # :8091
```

## Key endpoints
- `POST /api/v1/beneficiaries` — register a payout destination
- `GET  /api/v1/beneficiaries?partyType=&partyId=`
- `POST /api/v1/payouts` (headers `Idempotency-Key`, `X-Step-Up-Verified`) — create
- `GET  /api/v1/payouts/{id}`
- `POST /api/v1/payouts/{id}/approve` · `POST /api/v1/payouts/{id}/reject`

## Config
| Key | Default | Purpose |
|-----|---------|---------|
| `peb.payout.auto-approve-threshold-minor` | 5000000 (₹50,000) | above this → approval + step-up |
| `peb.payout.new-beneficiary-cooldown-hours` | 24 | recently changed beneficiary is high-risk |

> Sprint 6 models synchronous disbursement with failover; asynchronous payout webhook confirmation is
> a later refinement.

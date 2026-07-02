# purchase-expense-service

Records vendor **purchase bills** (computing input GST / ITC via the shared `GstCalculator`) and
business **expenses** (with maker-checker approval). Owns `purchase_db`. Port `8090`.

## APIs

- `POST /api/v1/purchase-bills` — record a bill; computes per-line taxable value and input GST.
- `GET  /api/v1/purchase-bills/{id}` — bill with items.
- `GET  /api/v1/purchase-bills?from=&to=` — list (date range optional).
- `POST /api/v1/expenses` — record an expense (`PENDING_APPROVAL`).
- `GET  /api/v1/expenses` — list.
- `POST /api/v1/expenses/{id}/approve` — checker approves (`APPROVED`); approver = JWT subject.
- `GET  /api/v1/purchase-register?from=&to=` — input-GST register rows.

## Events (outbox)

- `PURCHASE_BILL_CREATED` — payload `{purchaseBillId, vendorId, netMinor, inputGstMinor,
  totalAmountMinor, reverseCharge}`. The ledger consumer reads `netMinor` + `inputGstMinor`.
- `EXPENSE_APPROVED` — payload `{expenseId, amountMinor, category}`.

All amounts are integer paise. Tax math is delegated to `common-libraries` `GstCalculator`.

# Spec: Double-Entry Engine, Chart of Accounts & Posting Templates

Owning service: **accounting-ledger-service**. This is the financial core; correctness here is
the highest priority. Money is integer paise (see ADR-0005).

## 1. Invariants (enforced in service + DB)
1. Every journal entry has ≥2 lines; **Σ debit paise = Σ credit paise** (DB CHECK via trigger on commit).
2. No `UPDATE`/`DELETE` on `journal_entries`/`journal_entry_lines` by app role — corrections are new reversing entries.
3. Every entry references its `source_service` + `source_event_id` (traceability to invoice/payment/bill/payroll/cash/bank).
4. Posting is idempotent on `source_event_id` (unique) — replayed events never double-post.
5. Entries in a locked period are rejected unless carrying an approved `month_reopen` token.
6. `@Version` optimistic locking on `ledger_balances`.

## 2. Chart of Accounts (seed template, MSME-simplified)
Seeded per business on `BUSINESS_CREATED`. Account = `{code, name, type, normal_side}`.
Types: ASSET, LIABILITY, EQUITY, INCOME, EXPENSE. Normal side: ASSET/EXPENSE=Debit, others=Credit.

| Code | Account | Type | Normal |
|------|---------|------|--------|
| 1000 | Cash in Hand | ASSET | Dr |
| 1010 | Bank Account | ASSET | Dr |
| 1020 | UPI/Wallet Clearing | ASSET | Dr |
| 1100 | Accounts Receivable (Customers) | ASSET | Dr |
| 1200 | Input GST (ITC) — CGST/SGST/IGST | ASSET | Dr |
| 1300 | Inventory / Purchases | ASSET/EXPENSE | Dr |
| 1900 | Fixed Assets | ASSET | Dr |
| 1910 | Accumulated Depreciation | ASSET(contra) | Cr |
| 2000 | Accounts Payable (Vendors) | LIABILITY | Cr |
| 2100 | Output GST Payable — CGST/SGST/IGST | LIABILITY | Cr |
| 2200 | Employee Payable (Net Salary) | LIABILITY | Cr |
| 2210 | PF Payable / ESI Payable / PT Payable | LIABILITY | Cr |
| 2220 | TDS Payable | LIABILITY | Cr |
| 2300 | GST Payable (net) | LIABILITY | Cr |
| 2400 | Loan Liability | LIABILITY | Cr |
| 3000 | Owner Capital | EQUITY | Cr |
| 3100 | Owner Drawings | EQUITY(contra) | Dr |
| 4000 | Sales Revenue | INCOME | Cr |
| 4100 | Other Income | INCOME | Cr |
| 5000 | Cost of Goods Sold / Purchase Expense | EXPENSE | Dr |
| 5100 | Salary & Wages Expense | EXPENSE | Dr |
| 5200 | Rent / Utilities / Office Expense | EXPENSE | Dr |
| 5300 | Interest Expense | EXPENSE | Dr |
| 5400 | Depreciation Expense | EXPENSE | Dr |

## 3. Posting templates (source event → balanced journal)
Each template is a pure function `(event) → JournalEntry` in the domain layer, unit-tested to balance.

| Business event | Debit | Credit |
|----------------|-------|--------|
| Customer invoice (INVOICE_GENERATED) | 1100 Receivable (gross) | 4000 Sales (net) + 2100 Output GST (tax) |
| Customer payment (PAYMENT_RECEIVED) | 1010/1020/1000 Bank/UPI/Cash | 1100 Receivable |
| Vendor purchase (PURCHASE_BILL_CREATED) | 5000/1300 Purchase + 1200 Input GST | 2000 Vendor Payable |
| Vendor payment (VENDOR_PAYMENT_COMPLETED) | 2000 Vendor Payable | 1010/1020/1000 Bank/UPI/Cash |
| Salary run (SALARY_RUN_CREATED) | 5100 Salary Expense (gross) | 2200 Employee Payable (net) + 2210 PF/ESI/PT + 2220 TDS |
| Salary paid (payout complete) | 2200 Employee Payable | 1010 Bank |
| GST payment | 2300 GST Payable | 1010 Bank |
| Owner capital | 1010/1000 Bank/Cash | 3000 Owner Capital |
| Owner withdrawal | 3100 Drawings | 1010/1000 Bank/Cash |
| Loan EMI | 2400 Loan Liability + 5300 Interest Expense | 1010 Bank |
| Reversal/correction | mirror of original (swap Dr/Cr) referencing original entry id | — |

**GST split rule:** intra-state → CGST + SGST (half each of GST rate); inter-state → IGST (full).
Determined by place-of-supply vs business state. Banker's rounding at line then invoice level.

## 4. Financial period & month-lock state machine
```
OPEN ──draft close──▶ DRAFT_CLOSED ──lock (approved)──▶ LOCKED ──audited──▶ AUDITED
  ▲                                       │
  └──────── reopen (maker-checker) ◀───────┘   (reopen creates audit + APPROVAL_REQUESTED)
```
- OPEN: normal posting. DRAFT_CLOSED: posting allowed, flagged for review. LOCKED: no posting w/o
  approved reopen token. AUDITED: immutable except statutory-mandated adjustment via approval.
- `MONTH_LOCKED` / `MONTH_REOPEN_REQUESTED` events; reopen requires maker-checker + reason + evidence.

## 5. Cash vs accrual
Accrual is the book of record (invoice/bill create receivable/payable). Cash-basis view is a
derived report (recognizes income/expense on payment) computed in reporting layer — the underlying
journals are never rewritten.

## 6. Test obligations (Sprint 5 hard gate)
- Property test: for 10k randomized events, every produced journal balances.
- Reversal restores `ledger_balances` to pre-entry state exactly.
- Replayed `source_event_id` posts once.
- Posting into LOCKED period rejected; with approved reopen token accepted + audited.

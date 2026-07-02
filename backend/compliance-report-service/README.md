# compliance-report-service

Prepares GST/payroll/TDS/ITR compliance reports from a per-period read-model. These are
**preparation artifacts**, not statutory filings — the service never files with the tax portal.

## What it does

- **Report preparation** — aggregates period source rows (sales, purchases, payroll) into report
  lines and totals for report types `GSTR1_SUMMARY`, `GSTR3B_SUMMARY`, `SALES_REGISTER`,
  `PURCHASE_REGISTER`, `ITC_SUMMARY`, `TDS_SUMMARY`, `PAYROLL_COMPLIANCE`, `ITR_SUMMARY`.
- **Lifecycle** — `DRAFT → REVIEWED → APPROVED → FILED`, with maker-checker controls:
  - *Review* (reviewer) and *approve* (approver) require **distinct authorities** — a report is
    reviewed by an accountant/CA and approved by an owner.
  - *Approve* is additionally blocked until the underlying data is marked **reconciled**.
  - *FILED* is reached **only** by recording an official external portal/API acknowledgement
    (`ackReference`); recording it does **not** itself file with the portal.
- **Display state** — a report surfaces `UNRECONCILED` while its data is not reconciled (still
  DRAFT/REVIEWED), otherwise its status (`DRAFT`/`REVIEWED`/`APPROVED`/`FILED`).

## API

Base path `/api/v1/compliance`. All endpoints require a valid JWT (resource server). Port 8096,
database `compliance_db`.

- `POST /reports/generate` `{type, year, month}` — generate/regenerate a DRAFT report *(ACCOUNTANT/OWNER)*
- `GET  /reports` — list reports (newest first)
- `GET  /reports/{id}` — get a report
- `GET  /reports/{id}/lines` — list report lines
- `POST /reports/{id}/reconciled` `{reconciled}` — set the reconciled flag *(ACCOUNTANT/OWNER)*
- `POST /reports/{id}/review` — mark REVIEWED *(CA/ACCOUNTANT — maker-checker reviewer)*
- `POST /reports/{id}/approve` — approve *(OWNER/CO_OWNER — maker-checker approver, requires reconciled)*
- `POST /reports/{id}/filing` `{ackReference}` — record external acknowledgement → FILED *(OWNER/ACCOUNTANT)*

## Events consumed

Idempotent per `(recordType, sourceRef)`; at-least-once redeliveries are safe.

- `invoice.events` → `INVOICE_GENERATED` → SALES source (taxable = total − tax)
- `purchase.events` → `PURCHASE_BILL_CREATED` → PURCHASE source (taxable = net, tax = input GST)
- `payroll.events` → `SALARY_RUN_CREATED` → PAYROLL source (statutory, TDS)

Emits `COMPLIANCE_REPORT_GENERATED` via the transactional outbox.

## Notes

Invoice/purchase events do not carry the document/period date and the wire envelope carries no
`occurredAt`, so ingest-time (Asia/Kolkata) is used to derive year/month for those two. The payroll
event carries its own year/month and is used directly. A future event-schema enrichment should carry
the real period date.

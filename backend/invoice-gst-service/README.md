# invoice-gst-service

GST-compliant document service for the PEB platform (Java 21, Spring Boot 3.3).

## Responsibilities
- Issue GST documents: tax invoices, bills of supply, receipt vouchers, credit/debit notes.
- Compute GST (CGST/SGST vs IGST) via the pure `GstCalculator` engine — all amounts integer paise.
- Allocate gap-free, per-tenant/per-financial-year document numbers (`INV/2026-27/00001`).
- Render invoices/notes to PDF (OpenPDF).
- Produce e-invoice (IRP) and e-way-bill payloads in **readiness-only** mode (never filed).
- Emit `INVOICE_GENERATED` / `INVOICE_SENT` domain events via the transactional outbox.

## API
- `POST /api/v1/invoices` — create a document.
- `GET  /api/v1/invoices/{id}` — fetch with items + tax summary.
- `GET  /api/v1/invoices/{id}/pdf` — PDF (inline).
- `POST /api/v1/invoices/{id}/send` — mark sent.
- `GET  /api/v1/invoices/{id}/e-invoice-payload` — IRP readiness.
- `GET  /api/v1/invoices/{id}/eway-payload` — e-way-bill readiness.
- `POST /api/v1/invoices/credit-notes` / `/debit-notes` — notes against an original.
- `GET  /api/v1/gst/sales-register?from=&to=` — sales register for a date range.

## Config
- DB `invoice_db`, port `8088`.
- `peb.invoice.default-state-code`, `peb.invoice.prefixes.*` — numbering configuration.

The `GstCalculator` engine and Flyway migrations are the correctness core and must not be changed.

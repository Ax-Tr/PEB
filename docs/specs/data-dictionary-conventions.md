# Spec: Data Dictionary Conventions

Applies to every table in every service DB. Enforced by migration review + a shared checklist.

## 1. Standard columns (every tenant-scoped table)
| Column | Type | Notes |
|--------|------|-------|
| `id` | `char(26)` | ULID, app-generated, PK |
| `tenant_id` | `char(26)` | business/tenant scope; **NOT NULL**; in every index prefix |
| `branch_id` | `char(26)` NULL | when branch-scoped |
| `created_at` | `timestamptz` | UTC; display in IST |
| `updated_at` | `timestamptz` | UTC |
| `created_by` | `char(26)` | user id (actor) |
| `updated_by` | `char(26)` | user id |
| `version` | `bigint` | optimistic lock (`@Version`) on financial aggregates |
| `audit_event_id` | `char(26)` NULL | link to originating audit event |
| `source` / `source_event_id` | text / `char(26)` | provenance for financial rows |
| `status` | text | domain status enum |

Financial rows additionally: **no `deleted_at`** (no soft delete). Non-financial draft rows may use
`deleted_at timestamptz NULL` (soft delete only).

## 2. Money & numeric
- Monetary columns: `bigint` in **paise**, suffix `_minor` (e.g., `amount_minor`, `gst_amount_minor`).
- Never `float`/`double`/`real`/`money` for currency. Percentages as `numeric(5,2)`. Quantities `numeric(18,3)`.
- Currency fixed `INR` for v1 (column present for future).

## 3. Identifiers & keys
- ULID PKs. Natural business keys get UNIQUE constraints scoped by tenant: e.g.
  `unique(tenant_id, invoice_number, financial_year)`, `unique(gstin)`, `unique(tenant_id, mobile)`.
- Foreign keys only **within** a service DB. Cross-service references are opaque IDs (no FK).

## 4. Encryption tiers (field-level, KMS-backed)
| Tier | Fields | Handling |
|------|--------|----------|
| T1 secret | bank a/c no, IFSC, UPI ID, PAN, Aadhaar(if ever), card refs | encrypted at rest (field-level) + tokenized; masked in UI/logs by default; role-gated reveal + audit |
| T2 sensitive PII | mobile, email, GSTIN, address | encrypted at rest; masked by role; searchable via blind index/HMAC |
| T3 business-sensitive | amounts, ledgers, reports | DB-level AES-256 at rest; RBAC/ABAC gated |
| T4 public-ish | product names, HSN/SAC | standard |

Blind-index (HMAC) columns enable equality search on encrypted T2 fields (e.g., customer mobile
lookup) without decrypting.

## 5. Indexing baseline
- Every tenant table: `(tenant_id, <natural key>)` + `(tenant_id, created_at desc)` for feeds.
- Partial indexes on `status` for work queues (e.g., pending approvals, unmatched txns).
- BRIN on large append-only time-series (`bank_transactions`, `audit_events`, `outbox_events`).
- Foreign keys indexed; unique constraints on natural keys.

## 6. Shared infra tables (every service DB)
`outbox_events`, `processed_events`, `idempotency_keys`, `audit_events`, `flyway_schema_history`.
Grants: app role has **no DELETE** on `audit_events`, `journal_entries`, `journal_entry_lines`,
and other financial tables (append-only).

## 7. Time & locale
Store UTC `timestamptz`; render IST (Asia/Kolkata). Financial year = Apr–Mar (Indian FY) for
invoice numbering, GST periods, and reports.

# DPDP Data-Principal Rights (Sprint 16)

How PEB honours the Digital Personal Data Protection Act, 2023 (India) data-principal rights, and how
the flow is implemented in **privacy-service** (`/api/v1/privacy`, port 8101).

## Rights supported (Data Subject Requests)

| Right | DSR type | Notes |
|---|---|---|
| Access to personal data | `ACCESS` | Compile the principal's data for export. |
| Correction / completion | `CORRECTION` | Route correction to the owning service. |
| Erasure | `ERASURE` | Subject to statutory retention — see below. |
| Portability | `PORTABILITY` | Machine-readable export of the principal's data. |
| Grievance redressal | `GRIEVANCE` | Tracked to SLA; emits `DPDP_GRIEVANCE_RAISED`. |

## Request lifecycle (state machine)

`RECEIVED → VERIFYING → IN_PROGRESS → COMPLETED | REJECTED`

- **Identity verification is mandatory before any data is acted on** — this prevents an attacker from
  exercising another person's rights. `markVerified` moves the request to IN_PROGRESS.
- Every request carries a **statutory SLA due date** (`peb.privacy.sla-days`, default 30); the
  `GET /requests/overdue` endpoint surfaces breaches.
- Every transition is audited; the subject email is stored **field-level encrypted** and never logged.

## Erasure vs. retention — the honest rule

Erasure **never hard-deletes financial, tax, or KYC records** — Indian law (Income-tax, GST, PMLA)
requires their retention. `RetentionPolicy` + `ErasurePlan` classify each data category:

| Category | Outcome |
|---|---|
| MARKETING | DELETE (consent-based) |
| PROFILE_PII, CONTACT_PII | ANONYMIZE (record kept where linked to retained data) |
| KYC_PII | RETAIN under legal hold (~8y), then anonymise |
| FINANCIAL_TXN, TAX_RECORD | RETAIN under legal hold (~8y); linked PII anonymised |
| AUDIT_TRAIL | RETAIN (immutable, accountability) |

The erasure plan returns `fullErasurePossible=false` with a plain-language summary whenever retention
applies, so the response to the data principal is truthful about what will be deleted, anonymised, or
retained.

## Orchestration

`planErasure` emits **`DATA_ERASURE_REQUESTED`** with the plan. Each owning service acts on its own
slice — deleting deletable data and anonymising PII on retained records (using `Anonymizer`:
format-preserving masks and salted one-way pseudonyms). privacy-service does not reach into other
services' databases; it orchestrates via events and records fulfilment evidence on completion.

## Anonymisation

`Anonymizer` provides irreversible transforms: `name → REDACTED`, `email → redacted@domain`,
`phone → XXXXXXXXXX`, `pan → XXXXX0000X`, and `pseudonym(value, salt)` for a stable one-way reference.
There is no un-anonymise path by design.

## Gaps / follow-ups

- The requester-identity proof channel (email/OTP verification) is represented by the `verify` step;
  wiring it to the notification/OTP provider is an integration task.
- Per-service anonymisation consumers of `DATA_ERASURE_REQUESTED` are to be implemented per owning
  service as data volumes and schemas dictate.

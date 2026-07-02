# privacy-service

Implements the **DPDP (Digital Personal Data Protection Act) data-principal rights** workflow:
access, correction, erasure, portability and grievance requests, tracked against an SLA and
resolved with honest, audited evidence.

## What it does

- **Data-principal rights intake** — a data principal may raise a request of type `ACCESS`,
  `CORRECTION`, `ERASURE`, `PORTABILITY` or `GRIEVANCE`.
- **Identity verification first** — the request follows a small state machine
  (`RECEIVED → VERIFYING → IN_PROGRESS → COMPLETED | REJECTED`). The requester's identity must be
  verified (the `verify` step) **before any data is acted on**, preventing an attacker from
  exercising someone else's rights. The `verify` step represents the requester-identity verification
  integration (email/OTP proof).
- **Honest erasure plan** — erasure **never hard-deletes** financial, tax or KYC records. For each
  requested data category the plan states the action (`DELETE`, `ANONYMIZE`, or
  `RETAIN_LEGAL_HOLD`), the minimum statutory retention, and the reason. Financial/tax/KYC
  categories are retained under legal hold with their linked PII anonymised, so the plan reports
  `fullErasurePossible=false` honestly rather than promising a deletion it cannot perform.
- **Orchestration, not execution** — this service does **not** itself anonymise or delete data in
  other services. It emits `DATA_ERASURE_REQUESTED` and each owning service performs the actual
  per-service anonymisation/retention for its own slice of the data principal's data.
- **SLA tracking** — every request gets a due date (`peb.privacy.sla-days`, default 30 days).
  `GET /requests/overdue` surfaces SLA breaches for monitoring.
- **PII protection** — the subject's email is PII: encrypted at rest (via
  `EncryptedStringConverter`) and **never logged** (logs mask PII via the logback converter). It is
  returned in API responses only because every endpoint is restricted to the DPO function.
- **Auditing & tenancy** — every action is audited and all data is tenant-scoped.

## API

Base path `/api/v1/privacy`. All endpoints require a valid JWT (resource server) and are restricted
to the **Data Protection Officer function** — mapped to roles `OWNER`, `CO_OWNER`, `SUPPORT_ADMIN`
(there is no dedicated DPO role). GET endpoints are equally restricted because DSR data includes the
requester's email PII. Port **8101**, database `privacy_db`.

- `POST /requests` `{type, subjectRef?, subjectEmail, details?}` — intake a request
- `POST /requests/{id}/start-verification` — begin verifying the requester's identity
- `POST /requests/{id}/verify` — record that identity was verified
- `POST /requests/{id}/erasure-plan` `{categories:[...]}` — compute the erasure plan
- `POST /requests/{id}/complete` `{evidenceRef, note}` — complete with resolution evidence
- `POST /requests/{id}/reject` `{reason}` — reject with a reason
- `GET  /requests?status=` — list DSRs (optionally by status; newest first)
- `GET  /requests/overdue` — list DSRs past their SLA due date
- `GET  /requests/{id}` — get a DSR

All write endpoints and all GET endpoints require `hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')`.

## Events emitted

Published via the transactional outbox (this service **consumes nothing**):

- `DSR_RECEIVED` — a data-principal request was intaken
- `DATA_ERASURE_REQUESTED` — an erasure plan was computed; downstream services anonymise/retain
  their own slice in response (orchestration is here, execution is in each owning service)
- `DSR_COMPLETED` — a request was resolved with evidence
- `DPDP_GRIEVANCE_RAISED` — a `GRIEVANCE`-type request was intaken

## Notes

- The requester-identity verification integration (email/OTP proof) is represented by the `verify`
  step; the domain state machine enforces that `verify` precedes an erasure plan or completion.
- Financial/tax/KYC records are never hard-deleted — this is domain-enforced, and the plan/response
  surface `fullErasurePossible=false` when any such category is present.

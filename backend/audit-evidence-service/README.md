# audit-evidence-service

An immutable, append-only **evidence room** with SHA-256 integrity verification and auditor export
jobs.

## What it does

- **Append-only evidence** — evidence items are recorded and never updated or deleted. There is
  deliberately no update/delete endpoint; the domain has no setters and a database trigger enforces
  the same immutability. An evidence item may reference a **reversed transaction** — the proof
  survives the reversal.
- **Integrity verification** — every item stores a SHA-256 content hash. Uploaded artifacts hash the
  supplied bytes; system evidence hashes a stable canonical string of the source event's key fields.
  `POST /evidence/{id}/verify` recomputes the hash of a held artifact and reports whether it still
  matches — tamper detection any time after the fact.
- **System evidence from events** — cross-service domain events are turned into immutable evidence
  automatically. Recording is idempotent per (entity, hash), so at-least-once redeliveries do not
  duplicate evidence.
- **Auditor exports** — a small job lifecycle: `REQUESTED → PROCESSING → COMPLETED | FAILED`.
- **Emits `AUDIT_EVENT_RECORDED`** for every recorded evidence item (outbox relay).

## API

Base path `/api/v1/audit`. Port **8098**, database `audit_evidence_db`. All endpoints require a valid
JWT.

- `POST /evidence` — record uploaded evidence `{entityType, entityId, contentBase64, storageRef,
  description}` (write authority required).
- `GET /evidence?entityType=&entityId=` — list evidence for an entity.
- `GET /evidence/{id}` — get one evidence item.
- `POST /evidence/{id}/verify` — `{contentBase64}` → `{evidenceId, valid, storedHash}`.
- `POST /exports` — `{scope}` request an export (write authority required).
- `GET /exports` — list export jobs.
- `POST /exports/{id}/start`, `POST /exports/{id}/complete` `{resultRef}`, `POST /exports/{id}/fail`
  `{error}` — drive the export lifecycle (write authority required).

There is intentionally **no** `PUT`/`PATCH`/`DELETE` on evidence.

### Authorities

Recording evidence and the export lifecycle require `OWNER`, `CO_OWNER`, `ACCOUNTANT`, or `CA`. Read
and verify require only authentication, so an auditor holding a valid read token can inspect and
verify evidence. (There is no AUDITOR identity role — auditor scope is modeled in
ca-collaboration-service.)

## Events consumed

- `ledger.events` → `JOURNAL_ENTRY_POSTED` (payload `journalEntryId`, `amountMinor`, `entryDate`) →
  `JOURNAL_ENTRY` evidence. Recorded for every posting, including reversal postings.
- `payout.events` → `VENDOR_PAYMENT_COMPLETED` (payload `payoutId`, `partyId`, `amountMinor`) →
  `PAYOUT` evidence.

All other event types are acknowledged and skipped.

# ca-collaboration-service

A collaboration workspace for external accountants, CAs, and auditors working on a business's books.

## What it does

- **Role-scoped invitations** — the business owner invites an external collaborator as an
  `ACCOUNTANT`, `CA`, or `AUDITOR`. Access follows a small state machine (PENDING → ACCEPTED, or
  REVOKED / EXPIRED) and can be **revoked at any time, including mid-review**, after which the
  collaborator is immediately blocked. The invite email is **PII: encrypted at rest and never
  logged**.
- **Append-only review notes** — collaborators leave comments against an entity
  (`entityType` / `entityId`). Notes are never edited or deleted.
- **Maker-checker report approvals** — a maker (accountant / CA / owner) requests approval; a checker
  (owner / co-owner) decides it. The **approver must differ from the requester** (enforced in the
  domain *and* by a distinct checker authority). Requesting and deciding emit `APPROVAL_REQUESTED`
  and `APPROVAL_COMPLETED` via the transactional outbox.
- **Month-end close checklist** — a checklist of mandatory/optional items for a period. Its
  `canLockMonth` flag is true only when at least one mandatory item exists and every mandatory item
  is done; the ledger's month lock gates on this flag.
- **Auditor read-only scope** — the `AUDITOR` collaborator scope is strictly read-only, enforced at
  the service layer (`assertCanContribute` blocks read-only or revoked collaborators). There is no
  AUDITOR identity role.

Every state change is audited by the service layer.

## API

Base path `/api/v1/collaboration`. All endpoints require a valid JWT (resource server). Per-endpoint
authorities are enforced with `@PreAuthorize`:

| Endpoint | Authority |
| --- | --- |
| `POST /invites` | `OWNER`, `CO_OWNER` |
| `POST /invites/{id}/accept` | authenticated (the invitee) |
| `POST /invites/{id}/revoke` | `OWNER`, `CO_OWNER` |
| `GET /invites` | `OWNER`, `CO_OWNER` |
| `POST /notes` | `OWNER`, `CO_OWNER`, `ACCOUNTANT`, `CA` |
| `GET /notes?entityType=&entityId=` | authenticated |
| `POST /approvals` (maker) | `ACCOUNTANT`, `CA`, `OWNER`, `CO_OWNER` |
| `POST /approvals/{id}/decide` (checker) | `OWNER`, `CO_OWNER` |
| `GET /approvals?reportType=&reportRef=` | authenticated |
| `POST /checklists` | `OWNER`, `CO_OWNER`, `ACCOUNTANT`, `CA` |
| `POST /checklists/items/{itemId}` | `OWNER`, `CO_OWNER`, `ACCOUNTANT`, `CA` |
| `GET /checklists/{id}` | authenticated |
| `GET /checklists?year=&month=` | authenticated |

`acceptInvite` links the invite to the caller using the **JWT subject**
(`@AuthenticationPrincipal Jwt` → `jwt.getSubject()`), matching the sibling services.

Port 8099, database `ca_collaboration_db`.

## Events emitted

- `APPROVAL_REQUESTED` — a report approval was requested.
- `APPROVAL_COMPLETED` — a report approval was approved or rejected.

This service **consumes no Kafka topics**; it only emits via the outbox relay
(`peb.outbox.relay.enabled`, default true).

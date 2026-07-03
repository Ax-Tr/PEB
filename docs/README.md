# PayWithEase Business OS (PEB) — Engineering Documentation

> Indian MSME Finance Operating System. Java 21 + Spring Boot 3.x microservices backend,
> bare React Native + TypeScript mobile app, React + TypeScript web console.
>
> **Status:** Production-grade planning blueprint. All compliance content (GST, TDS, PF/ESI,
> e-invoice, e-way bill, DPDP, RBI/NPCI) MUST be validated with a qualified CA / legal /
> compliance professional and against the **current** official government/regulator portals
> before any statutory filing integration is enabled. The system never claims a statutory
> filing is complete without an official portal/API acknowledgement.

## How to read this pack

| # | Document | Covers (from the master build prompt) |
|---|----------|----------------------------------------|
| — | [decisions.md](./decisions.md) | Locked technology & product decisions + ADR log |
| 00 | [architecture-blueprint.md](./architecture-blueprint.md) | Outputs 1–9: architecture summary, module list, microservices map, database map, event map, API map, UI/UX screen map, security model, compliance model |
| 01 | [sprint-plan.md](./sprint-plan.md) | Output 10: full Sprint 0–19 execution plan (scope, stories, AC, services, DB, API, events, security, audit, tests, edge cases, failure scenarios, DoD) |
| 02 | [engineering-standards.md](./engineering-standards.md) | Outputs 12–15: repository structure, coding standards, testing plan, production readiness checklist |
| 03 | [sprint-0-instructions.md](./sprint-0-instructions.md) | Output 11: first-sprint implementation runbook |
| 04 | [specs/accounting-chart-of-accounts.md](./specs/accounting-chart-of-accounts.md) | Double-entry engine, CoA, posting templates, month-lock state machine |
| 05 | [specs/idempotency-outbox-saga.md](./specs/idempotency-outbox-saga.md) | Idempotency keys, transactional outbox, saga orchestration |
| 06 | [specs/threat-model.md](./specs/threat-model.md) | STRIDE threat model + control mapping |
| 07 | [specs/data-dictionary-conventions.md](./specs/data-dictionary-conventions.md) | ID/money/tenant/audit column conventions, encryption tiers |
| 08 | [pdf-gap-closure-sprint-plan.md](./pdf-gap-closure-sprint-plan.md) | Forward sprint plan for PDF comparison gaps: commitments, installment/reminder UX, real OCR bank capture, voice input, and gap analytics |

## Source of truth

The baseline requirements live in [`../PEB_MSME_FinTech_Document_Pack/`](../PEB_MSME_FinTech_Document_Pack/)
(10 DOCX documents: Product Strategy, PRD, FRD, TRD, Java Microservices Architecture,
Security/Privacy/Compliance, Data/API/Event, UI/UX, AI/Algorithms, Roadmap/QA/Launch).
These docs are **mandatory baseline requirements**. No baseline feature is removed; every
module is production-hardened. Where this pack and the DOCX pack disagree, this pack is the
implementation-level refinement and the discrepancy is logged in [decisions.md](./decisions.md).

## Non-negotiable product rules (enforced across all services)

1. No financial transaction is ever hard-deleted (soft-delete only for non-financial drafts).
2. Every edit produces an immutable audit trail entry.
3. Every posted accounting entry is reversible only via correction/reversal entries.
4. Every payment webhook is cryptographic-signature-verified.
5. Every financial action is idempotent (idempotency key + dedupe).
6. Every payout supports maker–checker approval driven by risk/amount/rule.
7. Every OCR result is user-reviewed before sensitive bank details are saved.
8. Every AI classification shows a confidence score.
9. Low-confidence AI suggestions are never auto-posted.
10. Month-locked books cannot be edited without an approval workflow.
11. Compliance reports always show state: `unreconciled | draft | reviewed | approved`.
12. The app never claims statutory filing is complete without official acknowledgement.
13. Secure-by-design, privacy-by-design, audit-by-design, compliance-by-design.

# Locked Decisions & Architecture Decision Records (ADR)

This file records decisions that change *what we build*. Each ADR is immutable once
`Accepted`; superseding decisions get a new ADR that references the old one.

## Locked decisions (this program)

| Area | Decision | Rationale |
|------|----------|-----------|
| Backend language | Java 21 (LTS), virtual threads where I/O-bound | Mandated by TRD; replaces the old Node.js baseline |
| Framework | Spring Boot 3.3.x, Spring Cloud 2023.x | REST, validation, actuator, security, batch |
| Build tool | **Gradle 8.x, Kotlin DSL, multi-module** | Faster incremental builds + build cache for 23 modules (ADR-0002) |
| API gateway | Spring Cloud Gateway | Auth enforcement, routing, throttling, correlation |
| Identity | Keycloak (primary) with Spring Authorization Server fallback | OAuth2/OIDC, device sessions, token rotation (ADR-0003) |
| Transactional DB | PostgreSQL 16, **database-per-service** | ACID financial data; no cross-service table access |
| Ledger/report queries | jOOQ over PostgreSQL | Type-safe complex SQL for accounting/reporting |
| Audit persistence | Custom append-only audit tables + Hibernate Envers on select aggregates | Immutable trail (ADR-0006) |
| Migrations | Flyway (per service, versioned `V__`/`R__`) | Deterministic, CI-gated |
| Analytics store | ClickHouse | Column store for dashboards/aggregations; OLTP stays clean |
| Search | OpenSearch | Audit/event/document full-text search |
| Object storage | S3-compatible (AWS S3 in prod, MinIO local) | Invoices, payslips, statements, OCR images, evidence, exports |
| Event bus | Kafka (MSK in prod, Redpanda/Kafka local) | Domain events + transactional outbox |
| Cache/locks | Redis | OTP, idempotency keys, rate limiting, distributed locks, read cache |
| Mapping | MapStruct | DTO ↔ domain ↔ entity |
| Resilience | Resilience4j | Circuit breaker, retry, bulkhead, rate limiter, time limiter |
| API docs | springdoc-openapi (OpenAPI 3.1), contract-first for external APIs | |
| Mobile | **Bare React Native + TypeScript** (Android priority) | Deeper native control: biometrics, secure keystore, camera/OCR (ADR-0004) |
| Web | React + TypeScript + Vite + Tailwind + TanStack Query | Role dashboards, ledger/recon/audit consoles |
| Money type | Integer **paise** (`BIGINT`) in DB; `BigDecimal` scale-2 in domain; never float | Financial correctness (ADR-0005) |
| IDs | ULID (lexicographically sortable) stored as `char(26)` / UUID at API edges | Sortable, index-friendly, no central sequence |
| Cloud | AWS first (EKS, RDS Multi-AZ, MSK, ElastiCache, S3, WAF, CloudFront) | TRD preference |
| IaC | Terraform + Helm | |
| Observability | OpenTelemetry → Prometheus/Grafana (metrics), Loki (logs), Tempo/Jaeger (traces) | |

## Open items requiring the business/CA before build of the relevant sprint

- Payment gateway(s): Razorpay vs Cashfree vs PhonePe PG — affects Payment Collection & Payout services (Sprint 3/6). Gateway abstraction built regardless.
- SMS/WhatsApp providers (WhatsApp BSP approval lead time) — Sprint 9.
- E-invoice/e-way bill: direct IRP/NIC integration vs GSP (ASP/GSP contract). Readiness payloads built now; live integration gated on contract (Sprint 12).
- DPDP data-fiduciary posture, retention periods per data class — Sprint 1 consent ledger, Sprint 16 hardening.
- Per-tenant KMS key strategy cost/feasibility on AWS KMS — Sprint 16.

---

## ADR-0001 — Microservices, database-per-service, no shared DB
**Status:** Accepted
**Context:** Build prompt & TRD mandate microservices, not a monolith; each service owns its
domain, DB, API contract, events, audit, and deploys independently.
**Decision:** 23 services (see architecture-blueprint.md §3). No service reads another
service's tables. Cross-service data flows via versioned REST (sync) or Kafka events (async).
Shared code lives only in `common-libraries` (no shared business tables).
**Consequences:** Eventual consistency between services; reconciliation & saga patterns
required; more operational surface. Accepted for scalability, blast-radius isolation, and
independent compliance auditing.

## ADR-0002 — Gradle Kotlin DSL multi-module over Maven
**Status:** Accepted
**Context:** 23 backend modules + common libs. TRD lists "Maven or Gradle".
**Decision:** Gradle 8 with Kotlin DSL, a version catalog (`gradle/libs.versions.toml`), and a
convention-plugins module (`build-logic/`) so every service shares Spotless, JaCoCo, testcontainers,
OpenAPI, and Boot config without copy-paste.
**Consequences:** Slightly steeper onboarding than Maven; offset by build cache & incremental builds.

## ADR-0003 — Keycloak as primary IdP
**Status:** Accepted
**Context:** OTP + passkey + device binding + RBAC/ABAC + token rotation + session revocation.
**Decision:** Keycloak (self-hosted on EKS) as OIDC provider. Identity Service owns OTP issuance,
device registry, session policy, and ABAC attributes; it federates auth to Keycloak realms
(one realm, tenant as a token claim). Spring Authorization Server kept as a documented fallback.
**Consequences:** Operational dependency on Keycloak; mitigated by HA deployment + regular backups.

## ADR-0004 — Bare React Native + TypeScript for mobile
**Status:** Accepted (supersedes the existing Expo prototype)
**Context:** User selected bare RN for native depth (biometric unlock, encrypted keystore,
camera OCR, deep links, offline-first). Existing Expo prototype (`../App.js`, `../src/`) is a
throwaway UI reference only.
**Decision:** New `mobile/` bare RN 0.7x TS workspace. Screens/theme from the prototype are
re-implemented in TS against real APIs. Prototype is retained under git history, not deleted.
**Consequences:** Manual native config (Gradle/Xcode); more setup than Expo; justified by
security-sensitive native requirements.

## ADR-0005 — Money as integer paise
**Status:** Accepted
**Context:** Floating point is unsafe for financial data.
**Decision:** All monetary DB columns are `BIGINT` in paise (₹1 = 100 paise). Domain uses
`Money` value object wrapping `long` paise + `Currency` (INR). API exposes both `amountMinor`
(paise, integer) and a formatted display string. GST/rounding uses banker's rounding at
line/invoice level per GST rules.
**Consequences:** All arithmetic in integer paise; conversion only at presentation edges.

## ADR-0006 — Immutable audit via append-only tables + outbox
**Status:** Accepted
**Context:** Rule: every edit → audit trail; audit events immutable; financial posts reversible.
**Decision:** Each service writes `audit_events` (append-only, no UPDATE/DELETE grants) in the
same DB transaction as the state change, and emits `AUDIT_EVENT_RECORDED` via the transactional
outbox. Audit & Evidence Service is the read/search aggregator (OpenSearch). DB role for app
users has no DELETE on financial/audit tables; corrections are new rows.
**Consequences:** Storage growth (managed via partitioning + lifecycle to cold storage, never
deletion within statutory retention).

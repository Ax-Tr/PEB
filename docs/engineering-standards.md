# PEB Engineering Standards

Covers required outputs 12 (repository structure), 13 (coding standards), 14 (testing plan),
15 (production readiness checklist).

---

## 1. Repository structure (mono-repo)

```
PEB/
├─ docs/                        # this pack (architecture, sprints, specs, ADRs)
├─ PEB_MSME_FinTech_Document_Pack/   # baseline DOCX requirements (source of truth)
├─ backend/
│  ├─ settings.gradle.kts       # includes all modules
│  ├─ build.gradle.kts          # root (applies convention plugins)
│  ├─ gradle/libs.versions.toml # version catalog (single source of dep versions)
│  ├─ build-logic/              # convention plugins (Kotlin DSL)
│  │  └─ src/main/kotlin/       # peb.java-conventions, peb.spring-service,
│  │                            # peb.testing, peb.openapi, peb.quality
│  ├─ common-libraries/         # shared lib module (NOT a service)
│  │  └─ src/main/java/com/paywithease/common/
│  │     ├─ money/  ids/  tenant/  correlation/  error/  event/
│  │     ├─ outbox/  idempotency/  security/  audit/  web/
│  ├─ api-gateway/
│  ├─ identity-service/
│  ├─ tenant-service/
│  ├─ customer-service/
│  ├─ vendor-service/
│  ├─ employee-payroll-service/
│  ├─ product-service/
│  ├─ payment-collection-service/
│  ├─ payout-service/
│  ├─ transaction-ingestion-service/
│  ├─ accounting-ledger-service/
│  ├─ invoice-gst-service/
│  ├─ purchase-expense-service/
│  ├─ installment-service/
│  ├─ notification-service/
│  ├─ ocr-document-service/
│  ├─ reconciliation-service/
│  ├─ compliance-report-service/
│  ├─ analytics-service/
│  ├─ audit-evidence-service/
│  ├─ ca-collaboration-service/
│  ├─ ai-automation-service/
│  └─ rules-engine-service/
├─ mobile/                      # bare React Native + TypeScript (Android priority)
│  ├─ src/{app,features,components,api,store,theme,native,offline}/
│  ├─ android/  ios/
├─ web/                         # React + TS + Vite + Tailwind + TanStack Query
│  └─ src/{app,features,components,api,store,theme}/
├─ infra/
│  ├─ docker/compose/           # local stack (postgres, redis, kafka, minio, keycloak, opensearch, clickhouse, otel)
│  ├─ helm/                     # per-service charts + umbrella
│  └─ terraform/                # modules: vpc, eks, rds, msk, elasticache, s3, waf, cloudfront, kms, iam
├─ tests/                       # cross-service contract, e2e, load (k6/Gatling), chaos
├─ security/                    # threat models, pen-test reports, SBOMs, policies
├─ scripts/                     # dev bootstrap, db seed, codegen, release
└─ .github/workflows/           # CI/CD pipelines
```

**Per-service internal structure (hexagonal / clean architecture):**
```
<service>/
├─ build.gradle.kts                    # applies peb.spring-service convention
├─ Dockerfile                          # distroless, multi-stage
├─ README.md                           # purpose, run, env vars, endpoints
├─ src/main/resources/
│  ├─ application.yml                  # + application-{local,dev,staging,prod}.yml
│  ├─ db/migration/                    # Flyway V__/R__
│  └─ openapi/<service>.yaml           # contract-first (external APIs)
└─ src/main/java/com/paywithease/<service>/
   ├─ api/            # controllers (thin), request/response DTOs, error mapping
   ├─ application/    # use-cases/services (orchestration, TX boundaries), ports
   ├─ domain/         # entities, value objects, domain events, domain services (no framework)
   ├─ infrastructure/ # JPA/jOOQ repositories, Kafka producers/consumers, gateway clients, outbox
   ├─ config/         # Spring config, security, resilience, observability
   └─ mapper/         # MapStruct mappers
```

**Branching strategy:** trunk-based with short-lived feature branches; `main` always releasable.
Conventional Commits; PR required; squash-merge; protected `main` (CI + review gates). Release tags
`vMAJOR.MINOR.PATCH`; per-service versioning via path-based CI matrix (only changed modules build).

---

## 2. Coding standards

**Java (all backend):**
1. Clean/hexagonal architecture — domain has **zero** framework imports; controllers are thin; no business logic in controllers.
2. DTO ↔ domain ↔ entity via MapStruct; validate DTOs with Jakarta Validation; never expose entities.
3. Money is `Money` value object over integer paise; never `double`/`float` for money.
4. IDs are ULID (`char(26)`); generated in domain, not DB.
5. Meaningful exceptions + stable error codes; global `@RestControllerAdvice` → RFC-7807 `problem+json`.
6. Structured JSON logs (Logback + Logstash encoder); correlation ID via MDC filter propagated to Kafka headers + downstream calls; **no PII/secrets in logs** (masking converter).
7. Explicit transaction boundaries in application layer; keep transactions short; no remote calls inside a DB TX.
8. Optimistic locking (`@Version`) on all financial aggregates; DB constraints enforce invariants (balanced entries via trigger/check, unique natural keys).
9. **Transactional outbox** for all event publishing; **idempotency** on all money/ledger mutations (Redis + DB unique key); consumers dedupe via `processed_events`.
10. **Saga** (orchestration) for multi-service financial workflows with explicit compensations.
11. Resilience4j on every outbound call (circuit breaker, retry w/ jitter, timeout, bulkhead).
12. Contract-first OpenAPI for externally consumed APIs; springdoc annotations everywhere; API versioned `/api/v1`.
13. Testcontainers for integration tests (real Postgres/Kafka/Redis); no H2 for financial logic.
14. Nullability: `Optional` for return absence; `@NonNull` defaults; fail fast on invalid state.
15. Formatting/linting enforced in CI: Spotless (google-java-format), SpotBugs, PMD, Error Prone.

**TypeScript (mobile + web):**
1. Strict mode; ESLint + Prettier; no `any` without justification.
2. Feature-folder structure; API layer via generated OpenAPI TS client (typed); TanStack Query for server state.
3. Money handled as integer paise + formatter; never float math on money.
4. Secure storage for tokens (Keychain/Keystore); no secrets in JS bundle; certificate pinning.
5. Every screen: loading/empty/error/success states; accessibility labels; 44×44 tap targets; i18n keys (en first).
6. Offline: encrypted local store; "Pending Sync" queue; conflict detection on sync; payment confirmation requires online.

**Every service ships:** README (purpose/run/env/endpoints), `application-local.yml`, Dockerfile,
Flyway migrations, OpenAPI, unit+integration tests, observability config, docker-compose entry.

---

## 3. Testing plan

**Pyramid & types**
| Layer | Tooling | Gate |
|-------|---------|------|
| Unit | JUnit 5, AssertJ, Mockito | ≥80% business logic, ≥90% accounting |
| Integration | Testcontainers (Postgres, Kafka, Redis, MinIO) | required per service |
| Contract | Spring Cloud Contract / Pact (provider+consumer) | must pass in CI |
| E2E | REST-assured (backend), Detox (mobile), Playwright (web) | key journeys |
| Payment webhook | signature + idempotency (double-delivery) suites | required |
| Reconciliation | weighted-match fixtures + golden files | required |
| Accounting balance | property test: Σdebits=Σcredits for every generated entry | **hard gate** |
| GST calc | matrix: intra/inter/exempt/RCM/zero-rated + rounding | required |
| Payroll calc | PF/ESI/PT/LOP/TDS matrix incl. joiner/leaver | required |
| Security | OWASP ZAP (API Top-10), MobSF (Mobile Top-10), dependency+secrets+container scan | no critical/high |
| Load | k6/Gatling — 10k concurrent staged, per-endpoint P95 SLO | SLOs met |
| Chaos | gateway/PG/Redis/Kafka outage injection | graceful degradation |
| OCR accuracy | labelled sample set, precision/recall thresholds | required |
| Accessibility | axe (web), RN a11y checks | required |
| UAT | MSME owner + accountant + CA scripted sessions | sign-off |
| DR drill | restore from backup + PITR + failover | rehearsed |

**Financial invariants asserted in tests:** balanced journals; no hard delete; reversal restores
balances; idempotent webhook/payout/ledger; month-lock enforcement; tenant isolation (cross-tenant
read returns 404/403); report status lifecycle correctness.

**Quality gates (block merge/release):** 80%/90% coverage; balanced-entry property test; no
critical/high security vulns; static analysis pass; dependency + secrets + container scan pass;
contract tests pass; staging smoke pass; rollback tested; no critical bugs open.

---

## 4. Production readiness checklist

**Security & compliance**
- [ ] TLS 1.3 edge, mTLS internal; WAF + bot protection enabled
- [ ] Secrets only in Vault/Secrets Manager; rotation configured; no secrets in images/repo
- [ ] Field-level encryption verified for PAN/bank/IFSC/UPI/GSTIN/mobile/email; KMS keys per policy
- [ ] PII masking by role in UI and logs verified
- [ ] Pen-test complete; no critical/high findings open; SBOM generated; images scanned
- [ ] DPDP: consent ledger, retention/deletion/anonymization flow, grievance path live
- [ ] RBI/data-localization: payment data stored in India region; data-flow map current
- [ ] Compliance reports show status; no "filed" without official ack; CA-review gating active

**Financial integrity**
- [ ] No hard-delete grants on financial/audit tables; append-only enforced
- [ ] Idempotency proven on payment/webhook/payout/ledger
- [ ] Maker-checker active on high-value payout, bank change, month reopen, manual adjustment, report approval, role elevation
- [ ] Reconciliation required before final compliance export
- [ ] Immutable audit events + evidence pack export working

**Reliability & ops**
- [ ] RDS Multi-AZ; automated encrypted backups; **PITR tested**; DR runbook rehearsed
- [ ] Kafka HA (MSK); outbox relay monitored; DLQ + replay in place
- [ ] Redis HA; graceful degradation when Redis/Kafka/S3/PG unavailable tested
- [ ] Autoscaling (HPA) configured; per-endpoint P95 SLOs defined + alerting
- [ ] Blue/green (or canary) deploy with automated rollback rehearsed
- [ ] Observability: OTel traces, Prometheus metrics, Loki logs, dashboards + alert rules live
- [ ] Separate dev/staging/UAT/prod; prod change via CI/CD only; release + rollback checklists signed
- [ ] Runbooks: incident response, on-call, gateway/PG failover, key rotation

**Product**
- [ ] All baseline PEB features present (no feature removed); UAT sign-off from MSME + accountant + CA
- [ ] App-store readiness (Android priority); deep links + push + biometrics verified
- [ ] Staging smoke + regression green; no critical bugs open

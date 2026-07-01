# PEB Backend

Java 21 · Spring Boot 3.3 · Gradle (Kotlin DSL) multi-module. Domain-driven microservices,
database-per-service, event-driven via Kafka + transactional outbox. See
[../docs/architecture-blueprint.md](../docs/architecture-blueprint.md) and
[../docs/engineering-standards.md](../docs/engineering-standards.md).

## Modules (Sprint 0)
| Module | Type | Purpose |
|--------|------|---------|
| `build-logic` | convention plugins | shared Java/Spring/testing/quality config |
| `common-libraries` | library | Money, ULID, TenantContext, correlation filter, RFC-7807 errors, event envelope, outbox, idempotency, audit, PII masking |
| `api-gateway` | service (:8080) | routing, correlation, rate-limit, circuit breaker |
| `identity-service` | service (:8081) | Sprint-0 skeleton (ping + baseline schema); Sprint-1 adds OTP/RBAC |

Further services are commented in [settings.gradle.kts](settings.gradle.kts) and enabled per sprint.

## Prerequisites
- **JDK 21** (Temurin). Check: `java -version`.
- **Docker** (for the local infra stack and Testcontainers-based tests).
- The Gradle wrapper is committed — use `./gradlew`, no local Gradle install needed.

## Quick start
```bash
# 1) Start infra (Postgres, Redis, Redpanda/Kafka, MinIO, Keycloak, OpenSearch, ClickHouse, OTel/Grafana)
../scripts/dev-up.sh

# 2) Build everything (runs Spotless, tests, coverage gate)
./gradlew build

# 3) Run services (two terminals)
./gradlew :identity-service:bootRun --args='--spring.profiles.active=local'
./gradlew :api-gateway:bootRun

# 4) Smoke test
../scripts/smoke.sh
```

## Common tasks
| Command | Does |
|---------|------|
| `./gradlew build` | compile + test + coverage verification for all modules |
| `./gradlew spotlessApply` | auto-format Java |
| `./gradlew :identity-service:test` | run one module's tests (Testcontainers Postgres) |
| `./gradlew printModules` | list modules |
| `./gradlew :identity-service:bootJar` | build runnable jar |

## Conventions (enforced)
- Money = integer paise via `Money` value object; never floats.
- IDs = ULID (`char(26)`), generated in domain.
- Every state change → outbox event + audit row in the same transaction.
- Idempotency on money/ledger mutations; append-only financial/audit tables.
- Hexagonal packages: `api / application / domain / infrastructure / config / mapper`.

## Notes / current limitations
- This machine has JDK 17 and no Docker, so the full `./gradlew build` was **not** executed here;
  it requires JDK 21 + Docker. The Gradle wrapper (8.10.2) and all module/build config are committed
  and standard. Run the Quick start on a JDK 21 + Docker host to verify end-to-end.

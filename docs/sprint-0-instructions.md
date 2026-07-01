# Sprint 0 — First-Sprint Implementation Runbook

Concrete, copy-ready instructions to stand up the foundation. This is what I execute as **real
code** in the next turn. Stack locked in [decisions.md](./decisions.md): Java 21, Spring Boot 3.3.x,
Gradle Kotlin DSL multi-module, PostgreSQL 16, Redis, Kafka/Redpanda, Keycloak, MinIO, OpenSearch,
ClickHouse, OTel.

## 0. Prerequisites (local dev machine)
- JDK 21 (Temurin), Docker Desktop, Node 20 + React Native CLI (for later mobile), `make`/bash.
- Verify: `java -version` → 21; `docker compose version`.

## 1. Target Sprint-0 tree (created next turn)
```
backend/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  build-logic/                     # convention plugins
    build.gradle.kts
    src/main/kotlin/
      peb.java-conventions.gradle.kts   # Java 21, Spotless, SpotBugs, JaCoCo
      peb.spring-service.gradle.kts     # Boot + web + actuator + otel + flyway + springdoc
      peb.testing.gradle.kts            # junit5, testcontainers, assertj, mockito
  common-libraries/
    build.gradle.kts
    src/main/java/com/paywithease/common/{money,ids,tenant,correlation,error,event,outbox,idempotency,security,audit,web}/
  api-gateway/           (Spring Cloud Gateway, routes to identity, JWKS validation, rate-limit)
  identity-service/      (skeleton: /api/v1/ping, actuator, Flyway baseline, outbox+audit tables)
infra/docker/compose/docker-compose.yml
.github/workflows/ci.yml
scripts/{dev-up.sh,dev-down.sh,smoke.sh}
```

## 2. `gradle/libs.versions.toml` (key entries)
```toml
[versions]
springBoot = "3.3.5"
springCloud = "2023.0.3"
mapstruct = "1.6.2"
resilience4j = "2.2.0"
testcontainers = "1.20.3"
ulid = "5.2.3"        # com.github.f4b6a3:ulid-creator
springdoc = "2.6.0"
jooq = "3.19.14"
[libraries]
# ... boot-starter-web/validation/actuator/security/data-jpa, kafka, redis, flyway,
#     mapstruct(+processor), resilience4j-spring-boot3, springdoc-openapi-starter-webmvc-ui,
#     ulid-creator, jooq, testcontainers(postgres,kafka), otel-spring-boot-starter
[plugins]
springBoot = { id = "org.springframework.boot", version.ref = "springBoot" }
springDepMgmt = { id = "io.spring.dependency-management", version = "1.1.6" }
spotless = { id = "com.diffplug.spotless", version = "6.25.0" }
```

## 3. `common-libraries` — what ships in Sprint 0
- `money/Money.java` — value object over `long` paise, `INR`, arithmetic + banker's rounding, `ofRupees`, `toMinor`, formatter.
- `ids/Ulid.java` — ULID generation + validation (`char(26)`).
- `tenant/TenantContext.java` — ThreadLocal/Scoped tenant + actor; populated by a gateway-forwarded header filter.
- `correlation/CorrelationFilter.java` — reads/creates `X-Correlation-Id`, sets MDC, propagates to Kafka headers.
- `error/` — `ApiError`, `@RestControllerAdvice` → RFC-7807 `problem+json`, stable error-code enum.
- `event/EventEnvelope.java` — the mandatory envelope (event_id, type, version, tenant_id, business_id, source_service, actor_id, correlation_id, causation_id, occurred_at, partition_key, payload).
- `outbox/` — `OutboxEvent` entity + `OutboxWriter` (same-TX insert) + `OutboxRelay` (polling `FOR UPDATE SKIP LOCKED` → Kafka).
- `idempotency/` — `IdempotencyKey` entity + `@Idempotent` aspect (Redis lock + DB unique).
- `audit/` — `AuditEvent` append-only entity + `AuditWriter` (same-TX) + `SERVICE_STARTED` demo.
- `security/` — resource-server JWT config helper, PII masking log converter, `@RequiresStepUp` marker.
- `web/` — pagination DTOs, base controller conventions, OpenAPI base config.

## 4. Baseline migration `V1__baseline.sql` (every service DB)
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE outbox_events (
  id char(26) PRIMARY KEY, aggregate_type text NOT NULL, aggregate_id char(26) NOT NULL,
  event_type text NOT NULL, event_version int NOT NULL DEFAULT 1, tenant_id char(26) NOT NULL,
  payload jsonb NOT NULL, headers jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now(),
  published_at timestamptz, attempts int NOT NULL DEFAULT 0);
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
CREATE TABLE processed_events (
  event_id char(26) NOT NULL, consumer text NOT NULL, processed_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (event_id, consumer));
CREATE TABLE idempotency_keys (
  tenant_id char(26) NOT NULL, key text NOT NULL, endpoint text NOT NULL, request_hash text NOT NULL,
  status text NOT NULL, response jsonb, created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, key));
CREATE TABLE audit_events (
  id char(26) PRIMARY KEY, tenant_id char(26) NOT NULL, actor_id char(26), event_type text NOT NULL,
  entity_type text, entity_id char(26), correlation_id text, occurred_at timestamptz NOT NULL DEFAULT now(),
  data jsonb NOT NULL DEFAULT '{}');
-- append-only: app role has no UPDATE/DELETE on audit_events
CREATE INDEX ix_audit_tenant_time ON audit_events (tenant_id, occurred_at DESC);
```

## 5. `docker-compose.yml` services (local stack)
postgres:16, redis:7, redpanda (Kafka API) + console, keycloak (dev realm import), minio (+mc bucket
init), opensearch + dashboards, clickhouse, otel-collector, prometheus, grafana, loki, tempo.
Healthchecks on each; `scripts/dev-up.sh` waits for health then runs `scripts/smoke.sh`.

## 6. CI pipeline `.github/workflows/ci.yml` (gates)
1. Detect changed modules (path filter) → build matrix.
2. `./gradlew spotlessCheck build test` (testcontainers via Docker service).
3. Static analysis: SpotBugs, PMD (report), Error Prone (compile).
4. Security: `gitleaks` (secrets), `trivy fs` + `trivy image` (deps + container), OWASP dependency-check.
5. Coverage gate (JaCoCo) — fail under 80% (90% for accounting module once it exists).
6. Contract tests (once services exist). Publish OpenAPI artifacts.
7. Build + scan Docker images; push to registry on `main`.

## 7. Definition of Done for Sprint 0
- `./gradlew build` green; `common-libraries` consumed by identity + gateway.
- `scripts/dev-up.sh` → full stack healthy → `scripts/smoke.sh` hits gateway → identity `/api/v1/ping` 200,
  a trace visible in Tempo, structured logs in Loki, metrics in Prometheus.
- Outbox relay publishes a `SERVICE_STARTED` event to Kafka; audit row written.
- CI runs all gates on PR. ADRs + this docs pack committed. Helm/Terraform skeletons present (not applied).

## 8. Immediately after Sprint 0
Proceed to **Sprint 1 (identity + tenant + RBAC)** using this same field-by-field runbook format,
cloning the identity/tenant service structure from the established pattern. Each subsequent service
is generated from the `peb.spring-service` convention + the hexagonal package template in
engineering-standards.md §1.
```
```

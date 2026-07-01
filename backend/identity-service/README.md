# identity-service

Identity & Access Service for PEB — OTP login, passkeys, JWT/OIDC (Keycloak), refresh-token
rotation, RBAC/ABAC, device binding, and session management. Owns `identity_db`.

**Sprint 0 (now):** skeleton only — `/api/v1/ping`, actuator health/prometheus, baseline schema
(outbox/processed/idempotency/audit), correlation + PII-masking + error handling from
`common-libraries`. **Sprint 1** adds the OTP/token/RBAC/onboarding flows.

## Run locally
```bash
# from backend/
../scripts/dev-up.sh                     # start infra (postgres, redis, kafka, ...)
./gradlew :identity-service:bootRun --args='--spring.profiles.active=local'
curl localhost:8081/api/v1/ping
curl localhost:8081/actuator/health
# API docs: http://localhost:8081/swagger-ui
```

## Environment variables
| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | 8081 | HTTP port |
| `DB_URL` | jdbc:postgresql://localhost:5432/identity_db | Postgres |
| `DB_USER` / `DB_PASSWORD` | peb / peb | DB creds (Vault in prod) |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | OTP store, locks, rate-limit |
| `KAFKA_BOOTSTRAP` | localhost:9092 | Event bus |

## Endpoints (Sprint 0)
- `GET /api/v1/ping` — service metadata
- `GET /actuator/health` · `GET /actuator/prometheus`
- `GET /swagger-ui` — OpenAPI UI

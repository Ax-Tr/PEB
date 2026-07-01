# tenant-service

Tenant/Business Service — business profile, branches, GST/PAN/Udyam identifiers, tax profile, and
invoice/tax settings. Owns `tenant_db`. `businesses.id` is the canonical **tenant_id** used across
every other service.

## Sprint 1 scope
- Create business (caller becomes owner); GSTIN (with check-digit), PAN, Udyam validation.
- Tax profile (GST registered, composition, reverse charge, TDS) and settings (invoice prefix, UPI, logo).
- Branches. Field-level encryption for GSTIN/PAN/UPI; GSTIN blind-index uniqueness.
- Emits `BUSINESS_CREATED`, `BUSINESS_SETTINGS_CHANGED` via the transactional outbox; audit on every change.
- Resource-server security: validates identity-service access tokens via its JWKS.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :tenant-service:bootRun          # :8082, validates JWTs against identity :8081 JWKS
```

## Environment variables
| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | 8082 | HTTP port |
| `DB_URL` | jdbc:postgresql://localhost:5432/tenant_db | Postgres |
| `IDENTITY_JWKS_URI` | http://localhost:8081/oauth2/jwks | token validation keys |
| `KAFKA_BOOTSTRAP` | localhost:9092 | event bus |

## Key endpoints
`POST /api/v1/businesses` · `GET/PATCH /api/v1/businesses/{id}` ·
`PUT /api/v1/businesses/{id}/tax-identifiers` · `PUT/GET /api/v1/businesses/{id}/tax-profile` ·
`PUT/GET /api/v1/businesses/{id}/settings` · `POST/GET /api/v1/businesses/{id}/branches`

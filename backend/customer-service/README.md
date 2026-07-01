# customer-service

Customer Service — customer directory (name, mobile, email, address, GSTIN), mobile lookup, contact
channels, and a per-customer ledger summary. Owns `customer_db`. Every customer is tenant-scoped to a
business (`tenant_id`).

## Sprint 2 scope
- Create customer (unique mobile per tenant, enforced by a blind index); Indian mobile validation.
- Search by mobile; list customers (newest first); get by id (tenant-scoped).
- Field-level encryption for mobile/email/GSTIN and contact values; mobile blind-index uniqueness.
- Ledger summary endpoint (stub: zeroed until invoice/ledger services publish balances).
- Emits `CUSTOMER_CREATED` via the transactional outbox; audit on every change.
- Resource-server security: validates identity-service access tokens via its JWKS.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :customer-service:bootRun        # :8083, validates JWTs against identity :8081 JWKS
```

## Environment variables
| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | 8083 | HTTP port |
| `DB_URL` | jdbc:postgresql://localhost:5432/customer_db | Postgres |
| `IDENTITY_JWKS_URI` | http://localhost:8081/oauth2/jwks | token validation keys |
| `KAFKA_BOOTSTRAP` | localhost:9092 | event bus |

## Key endpoints
`POST /api/v1/customers` · `GET /api/v1/customers` ·
`GET /api/v1/customers/search?mobile=` · `GET /api/v1/customers/{id}` ·
`GET /api/v1/customers/{id}/ledger-summary`

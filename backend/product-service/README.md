# product-service

Product/Item Service — the tenant's catalog of products (GOODs) and services (SERVICEs) with
HSN/SAC classification, GST rate, unit of measure, sale/purchase pricing (integer paise), and a
price-history trail. Owns `product_db`.

## Sprint 1 scope
- Create catalog line (GOOD → HSN, SERVICE → SAC); validates item type and GST slab.
- HSN/SAC master lookup to prefill the GST rate at catalog-entry time.
- Records an initial `price_history` row; monetary amounts are integer paise (`*_minor`).
- Emits `PRODUCT_CREATED` via the transactional outbox; audit on every change.
- Resource-server security: validates identity-service access tokens via its JWKS.

## Run locally
```bash
../scripts/dev-up.sh
./gradlew :product-service:bootRun          # :8085, validates JWTs against identity :8081 JWKS
```

## Environment variables
| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | 8085 | HTTP port |
| `DB_URL` | jdbc:postgresql://localhost:5432/product_db | Postgres |
| `IDENTITY_JWKS_URI` | http://localhost:8081/oauth2/jwks | token validation keys |
| `KAFKA_BOOTSTRAP` | localhost:9092 | event bus |

## Key endpoints
`POST /api/v1/products` · `GET /api/v1/products` · `GET /api/v1/products/{id}` ·
`GET /api/v1/hsn-sac?q=`

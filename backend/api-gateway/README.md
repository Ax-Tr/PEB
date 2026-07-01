# api-gateway

Edge for all PEB clients — routing, authentication enforcement, rate limiting, tenant resolution,
correlation IDs, API versioning. Reactive (Spring Cloud Gateway / WebFlux).

**Sprint 0 (now):** routes `/api/v1/**` identity paths to identity-service, injects
`X-Correlation-Id`, Redis-backed rate limiting, circuit breaker + fallback. **Sprint 1** adds JWT
(JWKS) validation, tenant-claim → `X-Tenant-Id` propagation, and per-principal rate-limit keys.

## Run locally
```bash
# from backend/ (infra + identity-service already running)
./gradlew :api-gateway:bootRun
curl localhost:8080/api/v1/ping        # proxied to identity-service
curl localhost:8080/actuator/health
```

## Environment variables
| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | 8080 | HTTP port |
| `IDENTITY_URI` | http://localhost:8081 | identity-service upstream |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | rate-limiter store |

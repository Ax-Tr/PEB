# OWASP API Security Top-10 (2023) — Control Mapping (Sprint 16)

How PEB addresses each risk. "Where" points at the concrete control in the codebase.

| # | Risk | Control in PEB |
|---|---|---|
| API1 | Broken Object Level Authorization (BOLA) | Every query is tenant-scoped via `TenantContext.requireTenantId()` and `findByTenantIdAnd…`; cross-tenant reads return not-found. Red-team tests in ai-automation & privacy services. |
| API2 | Broken Authentication | Edge JWT validation at the gateway (JWKS from identity-service) **and** per-service resource-server re-validation; OTP + refresh flows; step-up auth for sensitive actions. |
| API3 | Broken Object Property Level Authorization | Responses are explicit DTOs (no entity leakage); PII fields returned only on role-guarded endpoints; PII-masking log converter prevents property leakage to logs. |
| API4 | Unrestricted Resource Consumption | Gateway rate limiting (Redis) + circuit breakers/fallbacks; list endpoints paginated; analytics served from read-models, never OLTP. |
| API5 | Broken Function Level Authorization | `@EnableMethodSecurity` + `@PreAuthorize` per endpoint with least-privilege roles; maker-checker requires a **distinct** approver (domain-enforced). |
| API6 | Unrestricted Access to Sensitive Business Flows | High-risk flows (payouts, bank-detail change, month re-open, report approval, DSR erasure) gated by maker-checker and/or step-up; AI never auto-executes statutory/financial actions. |
| API7 | Server-Side Request Forgery | No user-supplied URLs are fetched server-side; outbound calls target fixed, configured provider endpoints. |
| API8 | Security Misconfiguration | Central `peb.*` convention plugins; strict security response headers at the edge (`SecurityHeadersGlobalFilter`: HSTS, nosniff, DENY frame, strict CSP, no-store, Permissions-Policy); server banner removed; CSRF disabled only because the API is token-based and stateless. |
| API9 | Improper Inventory Management | Single gateway route table is the API inventory; OpenAPI/springdoc per service; sprint-plan.md tracks delivered services and ports. |
| API10 | Unsafe Consumption of APIs | Signature-verified inbound webhooks; idempotent event consumption (dedupe by natural key); AI model output passed through `ConfidencePolicy` and never auto-applied when low-confidence/statutory; uploaded text scanned for prompt injection. |

## Mobile (OWASP MASVS) notes

- No secrets in the app bundle; tokens in secure storage; certificate pinning to the gateway.
- Field-level encryption means even a compromised backup never exposes plaintext PII.
- Bank-detail OCR results are always user-reviewed before saving (never auto-applied).

Gaps tracked in `threat-model.md` (pen-test sign-off, live KMS wiring, SIEM shipping, WAF tuning).

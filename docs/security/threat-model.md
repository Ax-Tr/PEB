# PEB Threat Model (Sprint 16)

Scope: the PayWithEase Business OS backend (21 Spring Boot microservices behind an API gateway,
PostgreSQL DB-per-service, Kafka event bus, Redis). Methodology: STRIDE per trust boundary, plus an
explicit mapping to OWASP API Security Top-10 (2023). This is a living document — update it whenever a
new trust boundary, external integration, or data class is introduced.

## Trust boundaries

1. **Internet → API gateway.** All external traffic terminates here. TLS, edge authn (JWT/JWKS),
   security response headers, correlation-id injection, and route-level public/authenticated split.
2. **Gateway → service mesh.** Internal calls carry the tenant/actor context forwarded as headers
   (`TenantContextFilter`). Services re-validate the JWT (resource server) — the gateway is not the
   only line of defence.
3. **Service → its own database.** DB-per-service; no service reads another service's DB. Field-level
   AES-GCM encryption for PII columns; blind indexes for equality lookup on encrypted data.
4. **Service → Kafka.** Transactional outbox → relay → topic → idempotent consumers. Events carry
   `tenantId`; consumers set tenant context from it and dedupe by natural key.
5. **Service → external providers** (payment gateway, notification/OTP, tax portals, KMS, model
   APIs). Signature-verified inbound webhooks; outbound secrets from the secret manager.

## STRIDE summary

| Threat | Example | Control |
|---|---|---|
| **Spoofing** | Forged caller / another tenant | JWT at gateway + per-service resource-server; `TenantContext.requireTenantId()` on every query; per-tenant data scoping (red-team tested in ai-automation & privacy). |
| **Tampering** | Altered financial record; forged webhook | No hard-delete of financial data (reversal-only); immutable audit + evidence room with SHA-256 + DB trigger blocking UPDATE/DELETE; HMAC signature verification on webhooks; Kafka outbox commits atomically with state. |
| **Repudiation** | "I didn't approve that" | Audit row on every state change (actor, correlation id, before/after); maker-checker for high-risk actions; immutable audit trail retained under legal hold. |
| **Information disclosure** | PII leak in logs/backups; cross-tenant read | Field-level encryption (PAN/Aadhaar/bank/IFSC/UPI/GSTIN/mobile/email); PII-masking log converter; DB-per-service; tenant scoping; strict CSP/no-store headers at edge. |
| **Denial of service** | Flood, expensive queries | Gateway rate limiting (Redis) + circuit breakers/fallbacks; analytics on read-models, never OLTP; pagination on list endpoints. |
| **Elevation of privilege** | Role escalation; approve own request | Role-scoped `@PreAuthorize`; maker-checker requires distinct approver (enforced in domain); step-up auth for sensitive actions; auditor collaborator scope is read-only. |

## Key abuse cases already mitigated (with tests)

- **Approve-your-own-report** → `ReportApproval.decide` rejects approver == requester.
- **Erase financial data** → `RetentionPolicy`/`ErasurePlan` force RETAIN_LEGAL_HOLD; erasure response is honest (`fullErasurePossible=false`).
- **Prompt injection via uploaded doc** → `PromptInjectionScanner` neutralises before any model call; AI is advisory (cannot file/post/pay).
- **Autonomous statutory filing** → `ConfidencePolicy` never auto-applies statutory kinds; "filed" requires an official acknowledgement (`ComplianceReport.recordFiling`).
- **Tamper with evidence** → append-only + SHA-256 verify + DB trigger.
- **Cross-tenant read** → tenant-scoped repositories; red-team tests in ai-automation and privacy services.

## Residual risks / follow-ups

- Live pen-test sign-off is external and pending (DoD item).
- Per-tenant KMS keys: strategy decided (see `secrets-and-key-management.md`); rollout via `KeyRing` versioning is staged.
- SIEM: structured audit + JSON logs are emitted; shipping/correlation rules are an ops integration.
- Rate-limit tuning and WAF rules are environment-specific and set at deploy time.

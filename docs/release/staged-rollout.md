# Staged Rollout Plan (Sprint 19)

Two independent dimensions of "staged": **infrastructure cutover** (blue/green canary — see
`deployment-and-rollback.md`) and **tenant/business exposure** (who can use the product), gated by
feature flags so exposure is decoupled from deploy.

## Tenant cohorts

| Stage | Cohort | Size | Gate to advance |
|---|---|---|---|
| 0 · Internal | Team + test tenants | ~10 | Golden path works; no critical bug 48 h. |
| 1 · Design partners | Hand-picked MSMEs + their CAs | 20–50 | UAT-style feedback positive; SLOs met; support load sane. |
| 2 · Early access | Waitlist, invite codes | 500 | Error budget intact; onboarding funnel healthy; reconciliation accurate at volume. |
| 3 · General availability | Open sign-up | All | Stage-2 metrics stable 2 weeks; capacity + on-call ready. |

## Feature-flag strategy

- Exposure controlled per-tenant by flags (not by deploy), so a feature can be dark-launched (deployed
  but off) and enabled per cohort. High-risk external integrations stay flagged off until contracts
  land (live payment gateway, e-invoice/e-way, WhatsApp) — see decisions.md open items.
- Kill-switch flags for each external provider so a provider outage degrades gracefully rather than
  failing the app.

## Guardrails during rollout

- Watch per-cohort: activation (first invoice/payment), error rate, P95 latency, Kafka lag/freshness,
  support tickets, and reconciliation exceptions.
- Auto-hold advancement if the error budget for the stage is spent or a financial-integrity alarm
  fires (unbalanced entry attempt, idempotency violation, cross-tenant access attempt).
- Roll back exposure (flag off) independently of the infra rollback if a product issue is cohort-wide.

## Onboarding analytics

The onboarding funnel (`../analytics/onboarding-funnel.md`) is the primary rollout health signal:
sign-up → business created → first invoice → first payment collected → first reconciliation → first
compliance report. Drop-off at any step gates cohort advancement and feeds product fixes.

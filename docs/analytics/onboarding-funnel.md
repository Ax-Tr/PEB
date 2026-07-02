# Onboarding Analytics — Activation Funnel (Sprint 19)

Defines the launch/onboarding funnel: the ordered activation milestones, the domain events that mark
each, and the metrics that gate the staged rollout. Analytics is event-fed (Sprint 13), so the funnel
is derived from existing domain events — **no new PII is collected**, and it is tenant-scoped like all
analytics.

## Funnel steps → source events

| # | Milestone | Marked by event (existing) | Emitting service |
|---|---|---|---|
| 1 | Signed up / first login | `USER_REGISTERED` / OTP verified | identity-service |
| 2 | Business created | `BUSINESS_CREATED` | tenant-service |
| 3 | First master added (customer/product) | `CUSTOMER_CREATED` / `PRODUCT_CREATED` | customer/product |
| 4 | First invoice issued | `INVOICE_GENERATED` | invoice-gst-service |
| 5 | First payment collected | `PAYMENT_RECEIVED` | payment-collection-service |
| 6 | First reconciliation | `RECONCILIATION_CONFIRMED` | reconciliation-service |
| 7 | First compliance report | `COMPLIANCE_REPORT_GENERATED` | compliance-report-service |
| 8 | CA/accountant invited | `CA_INVITE_CREATED` | ca-collaboration-service |

"Activated" tenant = reached step 5 (collected a payment). "Compliance-ready" = reached step 7.

## Metrics

- **Step conversion:** % of tenants advancing step N → N+1 (and time-to-advance, IST).
- **Activation rate:** % reaching step 5 within 7 / 30 days of sign-up.
- **Drop-off:** the step with the largest fall-off — the product-fix priority signal.
- **Time-to-value:** median time sign-up → first payment collected.
- **Cohort view:** funnel sliced by rollout cohort (see `../release/staged-rollout.md`) — gates cohort
  advancement.

## Implementation approach

- A funnel read-model is projected from the events above (same pattern as the Sprint 13 fact tables):
  one row per tenant tracking the furthest milestone + timestamps. It is **eventually consistent**;
  the analytics `/freshness` indicator applies.
- Dashboard: funnel bar (counts per step) + conversion/time-to-value + cohort filter. Served from the
  read-model, never OLTP; targets the < 3 s dashboard SLO.

## Status

- The funnel is **specified against events that already exist** (steps 2, 4, 5, 6, 7, 8 map to events
  emitted by delivered services). Step 1 depends on identity emitting a registration event
  (`USER_REGISTERED`) — a small addition flagged as a follow-up if not already emitted.
- Building the funnel read-model + dashboard is a fast-follow within analytics-service using the
  established projection pattern; it is not required for the technical launch gate but is the primary
  post-launch growth instrument (DoD: "post-launch monitoring dashboard live" — this is the product
  half; the ops half is the Grafana SLO dashboard).

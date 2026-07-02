# Launch Checklist (Sprint 19)

The final go/no-go gate for production launch. Complete every item; capture sign-offs. Builds on the
production-readiness assessment (`production-readiness-assessment.md`) — this checklist is the
launch-day execution list.

## T-1 week — pre-launch
- [ ] Production-readiness assessment reviewed; all 🟡 items resolved or risk-accepted with owner.
- [ ] Full backend suite green in CI (JDK 21); staging smoke + soak green.
- [ ] Pen-test sign-off; no critical/high open; SBOM archived; images scanned clean.
- [ ] UAT sign-off captured from MSME owner + accountant + CA.
- [ ] DR drill rehearsed: backup restore + PITR + region failover; RTO/RPO within target.
- [ ] Secrets in manager, rotation scheduled; per-tenant DEK/KEK provisioning verified.
- [ ] Alertmanager routes to on-call; alert rules (`infra/.../alerts.yml`) firing in staging tested.
- [ ] Grafana dashboards live (latency, error rate, Kafka lag, outbox backlog, DB pool, JVM).
- [ ] Runbooks current: incident, deploy/rollback, backup/DR, key rotation.
- [ ] Rollback rehearsed (traffic-only flip green→blue) with timing recorded.

## T-0 — cutover (blue/green, see deployment-and-rollback.md)
- [ ] Freeze non-release changes; announce the window.
- [ ] Apply expand (additive) migrations; verify BLUE still healthy.
- [ ] Deploy GREEN at 0 traffic; `kubectl rollout status` ready.
- [ ] Smoke GREEN (`smoke/smoke.sh`) — health, security headers, authn, golden-path reads.
- [ ] Canary 5% → 25% → 100%, baking on SLOs at each step (P95, error rate, Kafka lag, freshness).
- [ ] Automated rollback armed: any SLO breach flips traffic back to BLUE + pages on-call.

## T+0 — post-cutover
- [ ] 100% on GREEN and stable through the soak window; retire BLUE (contract migrations next release).
- [ ] Verify: a real invoice→payment→reconciliation→compliance flow end-to-end in prod (one pilot tenant).
- [ ] Confirm backups running on the new version; take a post-launch snapshot.
- [ ] Onboarding funnel dashboard live (see `../analytics/onboarding-funnel.md`); events flowing.
- [ ] Staged tenant rollout plan active (see `staged-rollout.md`).

## Go / No-go
- **Go** requires: every pre-launch box checked, smoke GREEN, rollback rehearsed, on-call staffed,
  and named sign-offs (Eng lead, Sec, DPO, Product). Any critical bug open = **No-go**.

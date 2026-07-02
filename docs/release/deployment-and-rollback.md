# Deployment & Rollback Runbook (Sprint 18)

Production changes go through CI/CD only — never manual `kubectl`/DB edits in prod. `main` is always
releasable; releases are tagged `vMAJOR.MINOR.PATCH`.

## Pre-deploy gate (must all be green)

- [ ] Full backend test suite green on JDK 21 (unit + integration) — see the Sprint 18 run.
- [ ] Spotless / static analysis / dependency + secrets + container scan pass.
- [ ] Flyway migrations reviewed: **backward-compatible** (expand/contract; no destructive change in
      the same release as the code that still reads the old shape).
- [ ] Config diff reviewed; all new config keys have safe defaults; secrets present in the manager.
- [ ] Staging smoke green (`release/smoke/smoke.sh`), staging soak clean.
- [ ] Rollback plan for this release confirmed (below); on-call briefed.

## Deploy (blue/green)

1. **Migrate (expand).** Apply additive Flyway migrations first; they are safe for the currently
   running (green) version. Never drop/rename a column in the same release that deploys the code
   change — do that one release later (contract).
2. **Deploy green.** Roll out the new version to the idle colour with zero traffic.
3. **Smoke green.** Run `smoke.sh` against the green colour's internal URL. Abort if any check fails.
4. **Shift traffic.** Cut the gateway/service mesh to green (or canary 5% → 25% → 100% watching P95,
   error rate, Kafka consumer lag, and the analytics freshness indicator).
5. **Observe.** Hold at each canary step until dashboards are within SLO for the bake time.
6. **Retire blue** once green is stable for the soak window.

## Rollback (rehearsed)

Triggers: SLO breach, error-rate spike, failed smoke, data-integrity alarm.

1. **Traffic rollback (fast path).** Shift the gateway back to blue (previous colour). This is the
   primary, seconds-level rollback and requires the DB to remain compatible — which the
   expand/contract migration discipline guarantees.
2. **Do NOT auto-run down-migrations.** Financial/audit data is append-only; forward-fix or a
   compensating migration is preferred over destructive rollback. Additive-only migrations mean blue
   still works against the new schema.
3. **Event replay.** If a bad consumer processed events, fix and replay from the DLQ / last committed
   offset; consumers are idempotent (natural-key dedupe) so replay is safe.
4. **Verify.** Re-run `smoke.sh`; confirm reconciliations and balances; check the immutable audit
   trail for the incident window.

## Rehearsal record

- Blue/green cutover and traffic rollback are rehearsed in staging each release; record the timing and
  outcome here as the DoD "rollback rehearsed" evidence.
- **Deferred (needs cluster):** the live blue/green mechanism, canary weights, and automated rollback
  triggers are Helm/mesh + CI concerns configured at deploy time; this runbook is the procedure they
  automate. Migrations are additive by construction so the traffic-only rollback is always available.

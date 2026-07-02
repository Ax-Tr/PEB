# Performance SLOs (Sprint 17)

Latency is measured at the API gateway (client-observed), steady state, warm caches, at the staged
target load (below). "Write" = endpoints that mutate state; "read" = GET/query endpoints;
"analytics" = dashboard queries served from read-models.

## Target load (staged)

| Stage | Concurrent users | Sustained RPS (approx) | Duration |
|---|---|---|---|
| Baseline | 500 | 300 | 10 min |
| Nominal | 2,000 | 1,200 | 30 min |
| Peak | 5,000 | 3,000 | 15 min |
| Stress | 10,000 | 6,000 | 10 min (find the knee) |
| Soak | 1,000 | 600 | 4 h (leak/GC check) |

## Latency SLOs (per class)

| Class | P50 | P95 | P99 | Error budget |
|---|---|---|---|---|
| Read (single entity GET) | 40 ms | 150 ms | 300 ms | 0.1% |
| Read (list, keyset-paginated) | 60 ms | 250 ms | 500 ms | 0.1% |
| Analytics dashboard (read-model) | 120 ms | **< 3 s** (AC) | 4 s | 0.5% |
| Write (simple, single aggregate) | 80 ms | 350 ms | 700 ms | 0.1% |
| Write (maker-checker / multi-step) | 120 ms | 500 ms | 1 s | 0.1% |
| Auth (OTP verify / token) | 100 ms | 400 ms | 800 ms | 0.1% |
| Heavy report / export | **async** — accepted < 200 ms; job completes < 60 s | — | — |

## Throughput & resource SLOs

- Kafka consumer lag < 5 s at peak (freshness indicator in analytics stays FRESH).
- DB connection pool saturation < 80% at peak; no query > 200 ms P95 on hot paths (indexed).
- No OLTP query issued by analytics (enforced by design: analytics reads its own read-model DB).
- JVM: no full-GC pause > 200 ms; heap steady over the 4 h soak (no leak).

## Enforcement / how these are met

- **Reads:** keyset pagination (`common.pagination.Cursor`/`Page`) keeps list latency flat as tables
  grow; per-endpoint covering indexes (see each service's `V*__performance_indexes.sql`).
- **Analytics off OLTP:** analytics-service consumes events into denormalised fact tables and only
  ever queries those; the `/freshness` endpoint surfaces read-model lag.
- **Heavy reports:** returned via a background job (async), the client polls status — never a blocking
  request (see `scaling-guide.md`, and audit-evidence `ExportJob`).
- **Caching:** tenant-scoped keys (`common.cache.CacheKeys`) with short TTLs on hot read-model
  aggregates; cache is a Redis cluster in prod, in-process Caffeine per pod for reference data.

## Reporting

Each load run produces a report (see `load-test-plan.md`) with the P50/P95/P99 table above, error
rates, Kafka lag, DB pool/CPU, and the identified stress knee. A run is a pass only if every class
meets its P95 SLO within its error budget. **Live runs require a deployed environment and are not
executed in this repo sandbox** — the k6 scripts under `k6/` are runnable against any environment.

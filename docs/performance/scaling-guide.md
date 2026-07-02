# Scaling & Performance Guide (Sprint 17)

How PEB meets its SLOs and scales to the staged 10k-concurrent target. This is the reference for the
performance controls implemented (and the ops levers to turn at deploy time).

## 1. Query optimisation & indexes

- Every hot list/filter query is backed by a covering index whose leading columns match the filter +
  sort order. Sprint 17 added `V*__performance_indexes.sql` for the "list all, order by time"
  endpoints whose existing `(tenant, status, time)` composites could not serve a tenant-only,
  order-by-time scan (compliance reports, CA invites, AI suggestions/anomalies, DSR requests).
- Rule of thumb: for `WHERE tenant_id = ? [AND col = ?] ORDER BY t DESC`, index
  `(tenant_id [, col], t DESC)`. Do not leave a filtered column between the tenant key and the sort
  key if an unfiltered variant of the query also exists.

## 2. Keyset (seek) pagination

- Use `common.pagination.Cursor` + `Page` for large lists. Fetch `limit + 1` rows ordered by
  `(sortCol DESC, id DESC)`, seeking past the cursor's `(sortValue, id)`; `Page.of(...)` derives the
  opaque `nextCursor`. Cost is independent of page depth — unlike `OFFSET`, which scans and discards.
- Clients treat the cursor as opaque and pass it back as `?cursor=`.

## 3. Caching

- Keys are always tenant-scoped via `common.cache.CacheKeys` (prevents cross-tenant cache
  poisoning). Never build a cache key by hand.
- **Prod topology:** Redis cluster for shared/distributed cache (dashboard aggregates, session/rate
  data); in-process Caffeine per pod for immutable reference data (HSN/SAC, GST rates) with short
  TTLs. Cache invalidation on the owning write path; short TTL as a backstop.
- Only cache read-model/derived data — never the financial system of record.

## 4. Analytics off OLTP

- analytics-service consumes domain events into denormalised fact tables and queries only those; it
  never reads another service's OLTP DB. The `/freshness` endpoint exposes read-model lag so
  dashboards can show staleness. Production OLAP target is ClickHouse (engine-agnostic read model).

## 5. Async jobs for heavy work

- Heavy reports/exports never block a request thread. The request enqueues a job (see audit-evidence
  `ExportJob`: REQUESTED→PROCESSING→COMPLETED|FAILED) and returns immediately; the client polls
  status and downloads the result artifact. Long CPU/IO work runs on a bounded worker pool /
  dedicated consumer, isolated from request-serving pods.

## 6. Kafka tuning

- Partition each topic for the target parallelism (partitions ≥ peak consumer instances per group).
  Partition key is `tenantId:aggregateId` → per-aggregate ordering preserved while spreading load.
- Consumer concurrency = partitions per instance; enable batch fetch for high-volume projection
  consumers (analytics, audit-evidence). Idempotent consumers (natural-key dedupe) make at-least-once
  safe.
- The transactional outbox relay decouples write latency from broker latency; tune relay batch size
  and poll interval so consumer lag stays < 5 s at peak.

## 7. Connection pools & DB

- Size HikariCP per pod so `pods × poolSize ≤ DB max_connections × safety`. Keep pool utilisation
  < 80% at peak. Use read replicas for read-heavy services (analytics already isolated).
- Statement timeouts on all pools; slow-query log enabled; no query > 200 ms P95 on hot paths.

## 8. Autoscaling

- Stateless services scale horizontally on CPU + P95 latency (HPA). Scale Kafka consumers on
  **consumer lag** (KEDA), not just CPU, so projection lag drives capacity.
- Gateway scales on RPS/latency; rate limiting (Redis token bucket) protects downstreams from
  overload and abusive tenants.
- DB scales vertically + read replicas; partition/shard the largest read-model tables by tenant when
  a single node is the knee (ClickHouse handles the analytics volume).

## 9. What is validated vs deferred

- **Implemented & unit-tested here:** keyset pagination, tenant-scoped cache keys, the performance
  indexes, async export job model, analytics read-model isolation, idempotent consumers.
- **Deferred (needs a deployed cluster):** the actual load-test runs (k6 scripts are ready under
  `k6/`), Redis/Caffeine wiring, HPA/KEDA manifests, and read-replica/ClickHouse provisioning — these
  are environment/ops tasks executed in staging for the Sprint 18 production-readiness sign-off.

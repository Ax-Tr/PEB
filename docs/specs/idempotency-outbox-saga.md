# Spec: Idempotency, Transactional Outbox & Saga

Reliability primitives shared by all services (implemented in `common-libraries`).

## 1. Idempotency (money & ledger mutations)
- Client sends `Idempotency-Key` header (UUID/ULID) on POST that moves money (payment request,
  webhook ingest, payout, installment pay, manual journal).
- Server flow: `INSERT INTO idempotency_keys(key, tenant_id, endpoint, request_hash, status)`
  with a UNIQUE constraint on `(tenant_id, key)`.
  - New key → process, store response, mark `COMPLETED`.
  - Duplicate key + same `request_hash` → return stored response (no re-execution).
  - Duplicate key + different hash → `409 Conflict` (idempotency key reuse with different body).
  - In-flight (`PROCESSING`) → `409` or `425 Too Early`, client retries with backoff.
- Redis holds a short-TTL lock `idem:{tenant}:{key}` to serialize concurrent duplicates; DB unique
  constraint is the durable guarantee. Keys retained ≥ 24h (payments) / configurable.
- **Webhooks:** provider event id is the idempotency key; signature verified *before* processing.

## 2. Transactional outbox
- State change + `INSERT INTO outbox_events(...)` happen in the **same DB transaction**.
- `outbox_events(id ULID, aggregate_type, aggregate_id, event_type, event_version, tenant_id,
  payload jsonb, headers jsonb, created_at, published_at NULL, attempts)`.
- A relay (Debezium CDC **or** a polling publisher with `SELECT ... FOR UPDATE SKIP LOCKED`)
  publishes unpublished rows to Kafka, sets `published_at`. At-least-once delivery.
- Consumers are idempotent: `processed_events(event_id, consumer, processed_at)` unique on
  `(event_id, consumer)`; a duplicate is a no-op.
- Poison messages → DLQ after N attempts with alert; replay tooling reads DLQ.

## 3. Saga orchestration (multi-service financial workflows)
Orchestrated (not choreographed) for auditability; each step has an explicit compensation.

**Receive → Invoice → Ledger → Installment saga**
1. `PAYMENT_RECEIVED` → orchestrator starts.
2. Generate invoice (invoice-gst). Compensation: void invoice (credit note if already sent).
3. Post ledger entry (accounting-ledger). Compensation: reversing entry.
4. If partial → create receivable schedule (installment). Compensation: cancel schedule.
5. Dispatch invoice (notification). Compensation: none (idempotent resend tolerated).
- Saga state persisted (`saga_instances`, `saga_steps`); each step idempotent; on failure the
  orchestrator runs compensations in reverse and emits an audit event + anomaly if manual attention needed.

**Purchase → Payout → Ledger saga**
1. Purchase bill created → (optional) approval.
2. Payout initiated (payout) — idempotent, maker-checker gate. Compensation: mark reversal/stop.
3. On `VENDOR_PAYMENT_COMPLETED` → post ledger. Compensation: reversing entry if settlement fails.
- If payout initiated but ledger post fails → compensation reverses; if payout gateway later
  confirms after a compensation, reconciliation-service raises an exception for manual resolution.

## 4. Failure-mode guarantees
| Failure | Behavior |
|---------|----------|
| Kafka down | outbox buffers; relay retries; no data loss; API still serves |
| Redis down | idempotency falls back to DB unique constraint (slower, still correct); OTP path degrades explicitly |
| PG failover | HikariCP retry + circuit breaker; in-flight TX rolls back cleanly |
| S3 down | document upload/export queued; signed-URL generation retried; never lose evidence |
| Consumer crash mid-batch | offsets committed only after `processed_events` insert; reprocessing is a no-op |
| Duplicate webhook | dedupe on provider event id → single effect |
| Saga step timeout | compensations run; state marked; anomaly/alert raised |

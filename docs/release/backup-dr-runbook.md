# Backup, PITR & DR Runbook (Sprint 19)

Covers backup policy, point-in-time recovery (PITR), and the disaster-recovery drill that must be
rehearsed before launch (DoD: "backups verified restorable; PITR tested").

## Backup policy

- **Databases (Postgres, DB-per-service):** managed automated backups (RDS/Cloud SQL) with
  transaction-log archiving for PITR. Full snapshot daily; WAL/binlog continuous. Encrypted at rest
  (KMS). Retention: 35 days snapshots + PITR window; monthly snapshot archived 8 years for the
  financial/audit retention obligation.
- **Object store (evidence, exports, invoice PDFs):** versioned bucket + cross-region replication;
  lifecycle to cold storage after 90 days; retained per the financial-records policy.
- **Kafka:** topics are a transport, not the system of record (the outbox + DB are). Retention sized
  to cover the max consumer outage + replay window; no "backup" of Kafka needed for correctness.
- **Config/secrets:** IaC in git; secrets in the manager with its own backup/replication.

## RPO / RTO targets

| Tier | RPO | RTO |
|---|---|---|
| Financial DBs (ledger, payment, payout, invoice) | ≤ 1 min (PITR) | ≤ 30 min |
| Other service DBs | ≤ 5 min | ≤ 1 h |
| Object store | ≤ replication lag | ≤ 1 h (failover to replica region) |

## PITR procedure

1. Identify the target timestamp (e.g. just before a bad deploy / data event).
2. Restore the DB to a **new** instance at that timestamp (never overwrite the primary).
3. Validate on the restored instance: row counts, latest ledger balances, `Σdebits=Σcredits` spot
   check, and that the immutable audit trail is intact.
4. Repoint the service (or promote) once validated. Because migrations are additive, an older restore
   is still schema-compatible with the running code.

## DR drill (rehearse before launch, then quarterly)

1. **Backup restorability:** restore the latest snapshot of each financial DB to a scratch instance;
   run `restore-drill.sh` to assert the schema and core invariants; record the restore time vs RTO.
2. **PITR:** pick a timestamp 1 h ago; restore; assert the expected data window is present.
3. **Region failover:** fail the primary region; promote the replica DB; bring services up in the DR
   region; run `docs/release/smoke/smoke.sh`; confirm SLOs. Record RTO/RPO achieved.
4. **Fail back:** return to primary; re-sync; confirm no data loss.

Record each drill's timings and outcome as the DoD evidence. Live drills require deployed
infrastructure and are executed in staging/DR (not in this repo sandbox); `restore-drill.sh` is the
repeatable validation used inside step 1/2.

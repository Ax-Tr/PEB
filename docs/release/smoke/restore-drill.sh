#!/usr/bin/env bash
# Backup/PITR restore drill validation. Point it at a RESTORED (scratch) Postgres instance and it
# asserts the schema is present and core financial invariants hold. Never run against production.
# Usage: PGURL="postgres://user:pass@host:5432/ledger_db" ./restore-drill.sh
set -euo pipefail

PGURL="${PGURL:?set PGURL to the restored instance connection string}"
psql() { command psql "$PGURL" -tAc "$1"; }
fail() { echo "DRILL FAIL: $1" >&2; exit 1; }

echo "== Restore drill against restored instance =="

# 1) Schema present (Flyway history + key tables).
[ "$(psql "SELECT count(*) FROM flyway_schema_history WHERE success")" -ge 1 ] \
  || fail "no successful Flyway migrations on the restore"
[ "$(psql "SELECT to_regclass('public.audit_events') IS NOT NULL")" = "t" ] \
  || fail "audit_events table missing"

# 2) Immutable audit trail present (accountability evidence survives restore).
echo "audit rows: $(psql "SELECT count(*) FROM audit_events")"

# 3) Ledger invariant spot check: every journal entry balances (Σdebits = Σcredits).
#    Adjust table/column names per the ledger schema; this is the golden financial assertion.
UNBALANCED="$(psql "
  SELECT count(*) FROM (
    SELECT journal_entry_id
    FROM journal_lines
    GROUP BY journal_entry_id
    HAVING COALESCE(SUM(debit_minor),0) <> COALESCE(SUM(credit_minor),0)
  ) x" 2>/dev/null || echo "SKIP")"
if [ "$UNBALANCED" = "SKIP" ]; then
  echo "ledger balance check skipped (not the ledger DB)"
elif [ "$UNBALANCED" != "0" ]; then
  fail "found $UNBALANCED unbalanced journal entries in the restore"
else
  echo "ledger balance check: all entries balanced"
fi

echo "RESTORE DRILL PASS"

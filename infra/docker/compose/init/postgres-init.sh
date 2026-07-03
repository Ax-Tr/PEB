#!/bin/sh
# Creates one database per microservice (database-per-service, ADR-0001).
# Runs automatically on first Postgres container start.
set -e

DBS="identity_db tenant_db customer_db vendor_db employee_db product_db \
payment_db payout_db ingestion_db ledger_db invoice_db purchase_db \
installment_db commitment_db notification_db ocr_db reconciliation_db compliance_db \
audit_db ca_collab_db ai_db rules_db"

for db in $DBS; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

#!/bin/bash
# Creates the three consolidated databases inside the same Postgres instance.
# Mounted as /docker-entrypoint-initdb.d/10-init-dbs.sh (runs once on first start).

set -e

for db in identity_db business_db finance_db analytics_db; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE $db OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

echo "All PEB databases created."

-- Auto-create all databases needed by PEB backend services.
-- PostgreSQL runs this on first startup only (via docker-entrypoint-initdb.d).

CREATE DATABASE identity_db;
CREATE DATABASE business_db;
CREATE DATABASE finance_db;

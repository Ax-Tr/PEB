-- V10: Align recon_items table with the ReconItem JPA entity.
-- Drop old indexes and constraints, drop/rename legacy columns, and add new fields.

-- Drop old index that uses run_id and status
DROP INDEX IF EXISTS ix_recon_items_run;

-- Drop foreign key constraint referencing recon_runs
ALTER TABLE recon_items DROP CONSTRAINT IF EXISTS recon_items_run_id_fkey;

-- Drop old legacy columns
ALTER TABLE recon_items DROP COLUMN IF EXISTS run_id;
ALTER TABLE recon_items DROP COLUMN IF EXISTS status;

-- Rename txn_date to item_date
ALTER TABLE recon_items RENAME COLUMN txn_date TO item_date;

-- Add new columns required by the entity
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS side text;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS direction text;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS reference text;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS counterparty text;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS narration text;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS matched boolean NOT NULL DEFAULT false;
ALTER TABLE recon_items ADD COLUMN IF NOT EXISTS match_id char(26);

-- Backfill defaults for any existing rows before making side and direction NOT NULL
UPDATE recon_items SET side = 'EXTERNAL' WHERE side IS NULL;
UPDATE recon_items SET direction = 'DEBIT' WHERE direction IS NULL;

ALTER TABLE recon_items ALTER COLUMN side SET NOT NULL;
ALTER TABLE recon_items ALTER COLUMN direction SET NOT NULL;

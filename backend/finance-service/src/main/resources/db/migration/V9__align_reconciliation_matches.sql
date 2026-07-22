-- V9: Align reconciliation_matches table columns with the ReconMatch entity.
-- The ReconMatch entity was refactored to use external_item_id, internal_item_id,
-- matched_by, and score instead of item_a_id, item_b_id, rule_id, and confidence.

ALTER TABLE reconciliation_matches RENAME COLUMN item_a_id TO external_item_id;
ALTER TABLE reconciliation_matches RENAME COLUMN item_b_id TO internal_item_id;
ALTER TABLE reconciliation_matches RENAME COLUMN rule_id TO matched_by;
ALTER TABLE reconciliation_matches RENAME COLUMN confidence TO score;

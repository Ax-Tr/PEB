-- V5__journal_entries_align.sql
-- Align journal_entries table columns with JournalEntry JPA entity definitions

ALTER TABLE journal_entries RENAME COLUMN source TO source_service;
ALTER TABLE journal_entries RENAME COLUMN source_ref TO source_event_id;
ALTER TABLE journal_entries ADD COLUMN reversal_of char(26);
ALTER TABLE journal_entries ADD COLUMN correlation_id text;

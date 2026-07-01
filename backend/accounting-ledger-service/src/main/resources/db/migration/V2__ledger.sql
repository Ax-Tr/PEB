-- Sprint 5 double-entry ledger schema. Financial rows are append-only (corrections are reversals);
-- the app DB role must have no UPDATE/DELETE on journal_entries / journal_entry_lines in production.

CREATE TABLE chart_of_accounts (
    id          char(26)    PRIMARY KEY,
    tenant_id   char(26)    NOT NULL,
    code        text        NOT NULL,
    name        text        NOT NULL,
    type        text        NOT NULL,                 -- ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    normal_side text        NOT NULL,                 -- DEBIT, CREDIT
    is_contra   boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);

CREATE TABLE financial_periods (
    id         char(26)    PRIMARY KEY,
    tenant_id  char(26)    NOT NULL,
    year       int         NOT NULL,
    month      int         NOT NULL,                  -- 1..12
    state      text        NOT NULL DEFAULT 'OPEN',   -- OPEN, DRAFT_CLOSED, LOCKED, AUDITED
    opened_at  timestamptz NOT NULL DEFAULT now(),
    locked_at  timestamptz,
    version    bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, year, month)
);

CREATE TABLE journal_entries (
    id              char(26)    PRIMARY KEY,
    tenant_id       char(26)    NOT NULL,
    entry_date      date        NOT NULL,
    narration       text,
    source_service  text,
    source_event_id char(26),
    reversal_of     char(26)    REFERENCES journal_entries(id),
    period_id       char(26)    REFERENCES financial_periods(id),
    status          text        NOT NULL DEFAULT 'POSTED',  -- POSTED, REVERSED
    correlation_id  text,
    created_by      char(26),
    created_at      timestamptz NOT NULL DEFAULT now()
);
-- Idempotency: one journal per source event (manual entries have null source_event_id).
CREATE UNIQUE INDEX ux_journal_source
    ON journal_entries (tenant_id, source_service, source_event_id)
    WHERE source_event_id IS NOT NULL;
CREATE INDEX ix_journal_tenant_date ON journal_entries (tenant_id, entry_date DESC);

CREATE TABLE journal_entry_lines (
    id               char(26)    PRIMARY KEY,
    tenant_id        char(26)    NOT NULL,
    journal_entry_id char(26)    NOT NULL REFERENCES journal_entries(id),
    account_id       char(26)    NOT NULL,
    account_code     text        NOT NULL,
    debit_minor      bigint      NOT NULL DEFAULT 0,
    credit_minor     bigint      NOT NULL DEFAULT 0,
    line_narration   text,
    CONSTRAINT ck_line_nonneg CHECK (debit_minor >= 0 AND credit_minor >= 0),
    CONSTRAINT ck_line_one_side CHECK (NOT (debit_minor > 0 AND credit_minor > 0))
);
CREATE INDEX ix_lines_entry ON journal_entry_lines (journal_entry_id);
CREATE INDEX ix_lines_account ON journal_entry_lines (tenant_id, account_id);

CREATE TABLE ledger_balances (
    id                 char(26)    PRIMARY KEY,
    tenant_id          char(26)    NOT NULL,
    account_id         char(26)    NOT NULL,
    account_code       text        NOT NULL,
    debit_total_minor  bigint      NOT NULL DEFAULT 0,
    credit_total_minor bigint      NOT NULL DEFAULT 0,
    version            bigint      NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, account_id)
);

-- Append-only lock/reopen log (maker-checker records land here + audit_events).
CREATE TABLE month_locks (
    id         char(26)    PRIMARY KEY,
    tenant_id  char(26)    NOT NULL,
    period_id  char(26)    NOT NULL REFERENCES financial_periods(id),
    action     text        NOT NULL,                  -- LOCK, REOPEN
    reason     text,
    actor_id   char(26),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- DB-level guarantee that every journal entry balances (Σdebit = Σcredit), checked at commit.
CREATE OR REPLACE FUNCTION assert_journal_balanced() RETURNS trigger AS $$
DECLARE d bigint; c bigint; n int;
BEGIN
  SELECT COALESCE(sum(debit_minor),0), COALESCE(sum(credit_minor),0), count(*)
    INTO d, c, n FROM journal_entry_lines WHERE journal_entry_id = NEW.journal_entry_id;
  IF n < 2 THEN
    RAISE EXCEPTION 'Journal % must have at least two lines', NEW.journal_entry_id;
  END IF;
  IF d <> c THEN
    RAISE EXCEPTION 'Journal % not balanced: debit=% credit=%', NEW.journal_entry_id, d, c;
  END IF;
  RETURN NULL;
END; $$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_balanced
    AFTER INSERT ON journal_entry_lines
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION assert_journal_balanced();

package com.paywithease.ledger.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.domain.FinancialPeriod;
import com.paywithease.ledger.domain.JournalCommand;
import com.paywithease.ledger.domain.JournalEntry;
import com.paywithease.ledger.domain.JournalEntryLine;
import com.paywithease.ledger.domain.LedgerBalance;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import com.paywithease.ledger.infrastructure.FinancialPeriodRepository;
import com.paywithease.ledger.infrastructure.JournalEntryLineRepository;
import com.paywithease.ledger.infrastructure.JournalEntryRepository;
import com.paywithease.ledger.infrastructure.LedgerBalanceRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The double-entry posting engine. Every post is balanced (guaranteed by {@link JournalCommand}),
 * idempotent on its source event, blocked in a locked period, and reflected in {@code
 * ledger_balances}. Corrections are new reversing entries — journals are never mutated or deleted.
 */
@Service
public class LedgerPostingService {

  private static final String SOURCE = "accounting-ledger-service";

  private final JournalEntryRepository entries;
  private final JournalEntryLineRepository lines;
  private final LedgerBalanceRepository balances;
  private final ChartOfAccountRepository accounts;
  private final FinancialPeriodRepository periods;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public LedgerPostingService(
      JournalEntryRepository entries,
      JournalEntryLineRepository lines,
      LedgerBalanceRepository balances,
      ChartOfAccountRepository accounts,
      FinancialPeriodRepository periods,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.entries = entries;
    this.lines = lines;
    this.balances = balances;
    this.accounts = accounts;
    this.periods = periods;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /**
   * Posts a balanced journal. Idempotent on (sourceService, sourceEventId). Returns the entry id.
   */
  @Transactional
  public String post(JournalCommand command, String createdBy) {
    return doPost(command, createdBy, null);
  }

  private String doPost(JournalCommand command, String createdBy, String reversalOf) {
    String tenantId = TenantContext.requireTenantId();
    command.validateBalanced();

    if (command.sourceEventId() != null) {
      var existing =
          entries.findByTenantIdAndSourceServiceAndSourceEventId(
              tenantId, command.sourceService(), command.sourceEventId());
      if (existing.isPresent()) {
        return existing.get().getId(); // already posted — idempotent no-op
      }
    }

    FinancialPeriod period =
        ensurePeriod(tenantId, command.entryDate().getYear(), command.entryDate().getMonthValue());
    if (!period.postingAllowed()) {
      throw new ApiException(
          ErrorCode.MONTH_LOCKED,
          "Period " + period.getYear() + "-" + period.getMonth() + " is locked");
    }

    JournalEntry entry =
        new JournalEntry(
            Ulid.newId(),
            tenantId,
            command.entryDate(),
            command.narration(),
            command.sourceService(),
            command.sourceEventId(),
            reversalOf,
            period.getId(),
            command.correlationId(),
            createdBy,
            clock.instant());
    entries.save(entry);

    for (JournalCommand.Line line : command.lines()) {
      ChartOfAccount account =
          accounts
              .findByTenantIdAndCode(tenantId, line.accountCode())
              .orElseThrow(
                  () ->
                      new ApiException(
                          ErrorCode.VALIDATION_FAILED,
                          "Account "
                              + line.accountCode()
                              + " not in chart of accounts (seed CoA)"));
      lines.save(
          new JournalEntryLine(
              Ulid.newId(),
              tenantId,
              entry.getId(),
              account.getId(),
              account.getCode(),
              line.debitMinor(),
              line.creditMinor(),
              line.narration()));
      applyToBalance(tenantId, account, line.debitMinor(), line.creditMinor());
    }

    audit.record(
        "JOURNAL_ENTRY_POSTED",
        "journal_entry",
        entry.getId(),
        Map.of(
            "amountMinor", command.totalDebitMinor(), "source", nullSafe(command.sourceService())));
    emit(entry, command.totalDebitMinor());
    return entry.getId();
  }

  /** Creates a reversing entry that exactly offsets the original. */
  @Transactional
  public String reverse(String entryId, String reason, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    JournalEntry original =
        entries
            .findByTenantIdAndId(tenantId, entryId)
            .orElseThrow(() -> ApiException.notFound("Journal entry"));
    if (entries.existsByTenantIdAndReversalOf(tenantId, entryId)) {
      throw new ApiException(ErrorCode.CONFLICT, "Entry already reversed");
    }

    var builder =
        JournalCommand.builder(
                java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneOffset.UTC),
                "Reversal of " + entryId + (reason == null ? "" : ": " + reason))
            .source(SOURCE, "reversal:" + entryId)
            .createdBy(actorId);
    for (JournalEntryLine line : lines.findByJournalEntryId(entryId)) {
      // Swap sides to offset the original.
      builder.debit(line.getAccountCode(), line.getCreditMinor(), "reversal");
      builder.credit(line.getAccountCode(), line.getDebitMinor(), "reversal");
    }
    String reversalId = doPost(builder.build(), actorId, entryId);
    audit.record("JOURNAL_REVERSED", "journal_entry", entryId, Map.of("reversalId", reversalId));
    return reversalId;
  }

  private FinancialPeriod ensurePeriod(String tenantId, int year, int month) {
    return periods
        .findByTenantIdAndYearAndMonth(tenantId, year, month)
        .orElseGet(
            () -> {
              try {
                return periods.saveAndFlush(
                    new FinancialPeriod(Ulid.newId(), tenantId, year, month, clock.instant()));
              } catch (DataIntegrityViolationException concurrent) {
                return periods
                    .findByTenantIdAndYearAndMonth(tenantId, year, month)
                    .orElseThrow(() -> concurrent);
              }
            });
  }

  private void applyToBalance(String tenantId, ChartOfAccount account, long debit, long credit) {
    LedgerBalance balance =
        balances
            .findByTenantIdAndAccountId(tenantId, account.getId())
            .orElseGet(
                () ->
                    new LedgerBalance(Ulid.newId(), tenantId, account.getId(), account.getCode()));
    balance.addDebit(debit);
    balance.addCredit(credit);
    balances.save(balance);
  }

  private void emit(JournalEntry entry, long amountMinor) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("journalEntryId", entry.getId());
    payload.put("amountMinor", amountMinor);
    payload.put("entryDate", entry.getEntryDate().toString());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType("JOURNAL_ENTRY_POSTED")
            .tenantId(entry.getTenantId())
            .businessId(entry.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(entry.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }

  /** Small read helper used by reports/tests. */
  @Transactional(readOnly = true)
  public List<LedgerBalance> balancesForTenant() {
    return balances.findByTenantId(TenantContext.requireTenantId());
  }
}

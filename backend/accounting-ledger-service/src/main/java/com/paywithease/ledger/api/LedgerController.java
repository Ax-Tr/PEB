package com.paywithease.ledger.api;

import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.application.CoaSeeder;
import com.paywithease.ledger.application.LedgerPostingService;
import com.paywithease.ledger.application.LedgerReportService;
import com.paywithease.ledger.application.MonthLockService;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.domain.FinancialPeriod;
import com.paywithease.ledger.domain.JournalCommand;
import com.paywithease.ledger.domain.JournalEntry;
import com.paywithease.ledger.domain.JournalEntryLine;
import com.paywithease.ledger.domain.LedgerBalance;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import com.paywithease.ledger.infrastructure.JournalEntryLineRepository;
import com.paywithease.ledger.infrastructure.JournalEntryRepository;
import com.paywithease.ledger.infrastructure.LedgerBalanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** General-ledger API: chart of accounts, journals, ledgers, reports, and month-lock. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "ledger", description = "Double-entry ledger, reports, and month-lock")
public class LedgerController {

  private final LedgerPostingService postingService;
  private final LedgerReportService reportService;
  private final MonthLockService monthLockService;
  private final CoaSeeder coaSeeder;
  private final ChartOfAccountRepository accounts;
  private final JournalEntryRepository entries;
  private final JournalEntryLineRepository lines;
  private final LedgerBalanceRepository balances;

  public LedgerController(
      LedgerPostingService postingService,
      LedgerReportService reportService,
      MonthLockService monthLockService,
      CoaSeeder coaSeeder,
      ChartOfAccountRepository accounts,
      JournalEntryRepository entries,
      JournalEntryLineRepository lines,
      LedgerBalanceRepository balances) {
    this.postingService = postingService;
    this.reportService = reportService;
    this.monthLockService = monthLockService;
    this.coaSeeder = coaSeeder;
    this.accounts = accounts;
    this.entries = entries;
    this.lines = lines;
    this.balances = balances;
  }

  // ---- Chart of accounts ----

  @GetMapping("/chart-of-accounts")
  @Operation(summary = "List the tenant's chart of accounts")
  public List<LedgerDtos.AccountResponse> chartOfAccounts() {
    return accounts.findByTenantIdOrderByCode(TenantContext.requireTenantId()).stream()
        .map(LedgerController::toAccount)
        .toList();
  }

  @PostMapping("/chart-of-accounts/seed")
  @Operation(summary = "Seed the standard MSME chart of accounts (idempotent)")
  public LedgerDtos.SeedResponse seed() {
    return new LedgerDtos.SeedResponse(coaSeeder.seedIfAbsent(TenantContext.requireTenantId()));
  }

  // ---- Journals ----

  @PostMapping("/journals")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Post a manual balanced journal entry")
  public LedgerDtos.JournalEntryResponse postJournal(
      @Valid @RequestBody LedgerDtos.PostJournalRequest body, @AuthenticationPrincipal Jwt jwt) {
    JournalCommand.Builder builder =
        JournalCommand.builder(body.entryDate(), body.narration()).createdBy(jwt.getSubject());
    for (LedgerDtos.JournalLineDto line : body.lines()) {
      if (line.debitMinor() > 0) {
        builder.debit(line.accountCode(), line.debitMinor(), line.narration());
      } else if (line.creditMinor() > 0) {
        builder.credit(line.accountCode(), line.creditMinor(), line.narration());
      }
    }
    String entryId = postingService.post(builder.build(), jwt.getSubject());
    return loadEntry(entryId);
  }

  @GetMapping("/journals/{id}")
  @Operation(summary = "Fetch a journal entry with its lines")
  public LedgerDtos.JournalEntryResponse getJournal(@PathVariable String id) {
    return loadEntry(id);
  }

  @PostMapping("/journals/{id}/reverse")
  @Operation(summary = "Post a reversing entry that offsets the original")
  public LedgerDtos.JournalEntryResponse reverseJournal(
      @PathVariable String id,
      @RequestBody(required = false) LedgerDtos.ReverseRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = body == null ? null : body.reason();
    String reversalId = postingService.reverse(id, reason, jwt.getSubject());
    return loadEntry(reversalId);
  }

  // ---- Ledgers ----

  @GetMapping("/ledgers/{accountCode}")
  @Operation(summary = "Running debit/credit totals for one account")
  public LedgerDtos.LedgerAccountResponse ledger(@PathVariable String accountCode) {
    String tenantId = TenantContext.requireTenantId();
    ChartOfAccount account =
        accounts
            .findByTenantIdAndCode(tenantId, accountCode)
            .orElseThrow(
                () -> com.paywithease.common.error.ApiException.notFound("Account " + accountCode));
    Optional<LedgerBalance> balance =
        balances.findByTenantIdAndAccountId(tenantId, account.getId());
    if (balance.isEmpty()) {
      return new LedgerDtos.LedgerAccountResponse(accountCode, 0, 0, 0);
    }
    LedgerBalance b = balance.get();
    return new LedgerDtos.LedgerAccountResponse(
        accountCode,
        b.getDebitTotalMinor(),
        b.getCreditTotalMinor(),
        b.netBalanceMinor(account.getNormalSide()));
  }

  // ---- Reports ----

  @GetMapping("/reports/trial-balance")
  @Operation(summary = "Trial balance (ties out because every journal balances)")
  public LedgerReportService.TrialBalance trialBalance() {
    return reportService.trialBalance();
  }

  @GetMapping("/reports/pnl")
  @Operation(summary = "Profit & loss (revenue − expense)")
  public LedgerReportService.ProfitAndLoss profitAndLoss() {
    return reportService.profitAndLoss();
  }

  @GetMapping("/reports/balance-sheet")
  @Operation(summary = "Balance sheet with current-period earnings carried into equity")
  public LedgerReportService.BalanceSheet balanceSheet() {
    return reportService.balanceSheet();
  }

  // ---- Month-lock ----

  @GetMapping("/periods/{year}/{month}")
  @Operation(summary = "Lock state of a financial period")
  public LedgerDtos.PeriodResponse periodStatus(@PathVariable int year, @PathVariable int month) {
    return toPeriod(monthLockService.status(year, month));
  }

  @PostMapping("/periods/{year}/{month}/lock")
  @Operation(summary = "Lock a period, blocking further posting")
  public LedgerDtos.PeriodResponse lockPeriod(
      @PathVariable int year,
      @PathVariable int month,
      @RequestBody(required = false) LedgerDtos.PeriodActionRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = body == null ? null : body.reason();
    return toPeriod(monthLockService.lock(year, month, reason, jwt.getSubject()));
  }

  @PostMapping("/periods/{year}/{month}/reopen")
  @Operation(summary = "Reopen a locked period (maker-checker)")
  public LedgerDtos.PeriodResponse reopenPeriod(
      @PathVariable int year,
      @PathVariable int month,
      @RequestBody(required = false) LedgerDtos.PeriodActionRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = body == null ? null : body.reason();
    return toPeriod(monthLockService.reopen(year, month, reason, jwt.getSubject()));
  }

  // ---- Mapping helpers ----

  private LedgerDtos.JournalEntryResponse loadEntry(String entryId) {
    JournalEntry entry =
        entries
            .findByTenantIdAndId(TenantContext.requireTenantId(), entryId)
            .orElseThrow(() -> com.paywithease.common.error.ApiException.notFound("Journal entry"));
    List<LedgerDtos.JournalLineResponse> lineResponses =
        lines.findByJournalEntryId(entry.getId()).stream().map(LedgerController::toLine).toList();
    return new LedgerDtos.JournalEntryResponse(
        entry.getId(),
        entry.getEntryDate(),
        entry.getNarration(),
        entry.getSourceService(),
        entry.getSourceEventId(),
        entry.getStatus(),
        entry.getReversalOf(),
        lineResponses);
  }

  private static LedgerDtos.JournalLineResponse toLine(JournalEntryLine line) {
    return new LedgerDtos.JournalLineResponse(
        line.getAccountCode(), line.getDebitMinor(), line.getCreditMinor(), null);
  }

  private static LedgerDtos.AccountResponse toAccount(ChartOfAccount a) {
    return new LedgerDtos.AccountResponse(
        a.getCode(), a.getName(), a.getType().name(), a.getNormalSide().name(), a.isContra());
  }

  private static LedgerDtos.PeriodResponse toPeriod(FinancialPeriod p) {
    return new LedgerDtos.PeriodResponse(p.getYear(), p.getMonth(), p.getState().name());
  }
}

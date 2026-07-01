package com.paywithease.ledger.application;

import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.AccountType;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.domain.LedgerBalance;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import com.paywithease.ledger.infrastructure.LedgerBalanceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives the standard financial statements from {@code ledger_balances}. Because every journal
 * balances, the trial balance always ties out and the balance sheet balances once current-period
 * earnings (the P&amp;L result) are carried into equity.
 */
@Service
public class LedgerReportService {

  private final LedgerBalanceRepository balances;
  private final ChartOfAccountRepository accounts;

  public LedgerReportService(LedgerBalanceRepository balances, ChartOfAccountRepository accounts) {
    this.balances = balances;
    this.accounts = accounts;
  }

  public record TrialBalanceRow(String code, String name, long debitMinor, long creditMinor) {}

  public record TrialBalance(
      List<TrialBalanceRow> rows, long totalDebitMinor, long totalCreditMinor, boolean balanced) {}

  public record ProfitAndLoss(long revenueMinor, long expenseMinor, long netProfitMinor) {}

  public record BalanceSheet(
      long assetsMinor,
      long liabilitiesMinor,
      long equityMinor,
      long currentEarningsMinor,
      long totalEquityAndLiabilitiesMinor,
      boolean balanced) {}

  @Transactional(readOnly = true)
  public TrialBalance trialBalance() {
    Map<String, ChartOfAccount> byCode = accountsByCode();
    List<TrialBalanceRow> rows = new ArrayList<>();
    long totalDebit = 0;
    long totalCredit = 0;
    for (LedgerBalance b : balances.findByTenantId(TenantContext.requireTenantId())) {
      long net = b.getDebitTotalMinor() - b.getCreditTotalMinor();
      long debit = Math.max(0, net);
      long credit = Math.max(0, -net);
      ChartOfAccount acc = byCode.get(b.getAccountCode());
      rows.add(
          new TrialBalanceRow(b.getAccountCode(), acc == null ? "" : acc.getName(), debit, credit));
      totalDebit += debit;
      totalCredit += credit;
    }
    return new TrialBalance(rows, totalDebit, totalCredit, totalDebit == totalCredit);
  }

  @Transactional(readOnly = true)
  public ProfitAndLoss profitAndLoss() {
    long[] byType = totalsByType();
    long revenue = byType[AccountType.INCOME.ordinal()];
    long expense = byType[AccountType.EXPENSE.ordinal()];
    return new ProfitAndLoss(revenue, expense, revenue - expense);
  }

  @Transactional(readOnly = true)
  public BalanceSheet balanceSheet() {
    long[] byType = totalsByType();
    long assets = byType[AccountType.ASSET.ordinal()];
    long liabilities = byType[AccountType.LIABILITY.ordinal()];
    long equity = byType[AccountType.EQUITY.ordinal()];
    long earnings = byType[AccountType.INCOME.ordinal()] - byType[AccountType.EXPENSE.ordinal()];
    long totalEqAndLiab = liabilities + equity + earnings;
    return new BalanceSheet(
        assets, liabilities, equity, earnings, totalEqAndLiab, assets == totalEqAndLiab);
  }

  /**
   * Net balance per account type (each on its own normal side), indexed by AccountType.ordinal().
   */
  private long[] totalsByType() {
    Map<String, ChartOfAccount> byCode = accountsByCode();
    long[] totals = new long[AccountType.values().length];
    for (LedgerBalance b : balances.findByTenantId(TenantContext.requireTenantId())) {
      ChartOfAccount acc = byCode.get(b.getAccountCode());
      if (acc == null) {
        continue;
      }
      totals[acc.getType().ordinal()] += b.netBalanceMinor(acc.getType().normalSide());
    }
    return totals;
  }

  private Map<String, ChartOfAccount> accountsByCode() {
    return accounts.findByTenantIdOrderByCode(TenantContext.requireTenantId()).stream()
        .collect(Collectors.toMap(ChartOfAccount::getCode, Function.identity(), (a, b) -> a));
  }
}

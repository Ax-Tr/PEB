package com.paywithease.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.AccountType;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.domain.LedgerBalance;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import com.paywithease.ledger.infrastructure.LedgerBalanceRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerReportServiceTest {

  @Mock LedgerBalanceRepository balances;
  @Mock ChartOfAccountRepository accounts;
  private LedgerReportService service;
  private static final Instant NOW = Instant.parse("2026-05-15T00:00:00Z");

  @BeforeEach
  void setUp() {
    service = new LedgerReportService(balances, accounts);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));

    // Scenario: invoice ₹1180 (Recv Dr 118000; Sales Cr 100000; Output GST Cr 18000),
    // then full payment (UPI Dr 118000; Recv Cr 118000).
    when(accounts.findByTenantIdOrderByCode("tenant1"))
        .thenReturn(
            List.of(
                account("1020", "UPI Clearing", AccountType.ASSET),
                account("1100", "Accounts Receivable", AccountType.ASSET),
                account("2100", "Output GST", AccountType.LIABILITY),
                account("4000", "Sales Revenue", AccountType.INCOME)));
    when(balances.findByTenantId("tenant1"))
        .thenReturn(
            List.of(
                balance("1020", 118000, 0), // UPI net debit 118000
                balance("1100", 118000, 118000), // Receivable net 0
                balance("2100", 0, 18000), // Output GST net credit 18000
                balance("4000", 0, 100000))); // Sales net credit 100000
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void trialBalanceTiesOut() {
    var tb = service.trialBalance();
    assertThat(tb.balanced()).isTrue();
    assertThat(tb.totalDebitMinor()).isEqualTo(118000);
    assertThat(tb.totalCreditMinor()).isEqualTo(118000);
  }

  @Test
  void profitAndLossShowsRevenue() {
    var pnl = service.profitAndLoss();
    assertThat(pnl.revenueMinor()).isEqualTo(100000);
    assertThat(pnl.expenseMinor()).isZero();
    assertThat(pnl.netProfitMinor()).isEqualTo(100000);
  }

  @Test
  void balanceSheetBalancesWithCurrentEarnings() {
    var bs = service.balanceSheet();
    assertThat(bs.assetsMinor()).isEqualTo(118000);
    assertThat(bs.liabilitiesMinor()).isEqualTo(18000);
    assertThat(bs.currentEarningsMinor()).isEqualTo(100000);
    assertThat(bs.totalEquityAndLiabilitiesMinor()).isEqualTo(118000);
    assertThat(bs.balanced()).isTrue();
  }

  private static ChartOfAccount account(String code, String name, AccountType type) {
    return new ChartOfAccount("acc-" + code, "tenant1", code, name, type, false, NOW);
  }

  private static LedgerBalance balance(String code, long debit, long credit) {
    LedgerBalance b = new LedgerBalance("bal-" + code, "tenant1", "acc-" + code, code);
    b.addDebit(debit);
    b.addCredit(credit);
    return b;
  }
}

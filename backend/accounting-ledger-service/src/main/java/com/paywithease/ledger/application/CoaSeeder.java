package com.paywithease.ledger.application;

import com.paywithease.common.ids.Ulid;
import com.paywithease.ledger.domain.AccountType;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the MSME-simplified chart of accounts for a business (specs/accounting-chart-of-accounts.md
 * §2). Invoked once per tenant — on the {@code BUSINESS_CREATED} event or explicitly — and is
 * idempotent (a tenant that already has accounts is skipped).
 */
@Service
public class CoaSeeder {

  private record Seed(String code, String name, AccountType type, boolean contra) {}

  private static final List<Seed> TEMPLATE =
      List.of(
          new Seed("1000", "Cash in Hand", AccountType.ASSET, false),
          new Seed("1010", "Bank Account", AccountType.ASSET, false),
          new Seed("1020", "UPI/Wallet Clearing", AccountType.ASSET, false),
          new Seed("1100", "Accounts Receivable", AccountType.ASSET, false),
          new Seed("1200", "Input GST (ITC)", AccountType.ASSET, false),
          new Seed("1300", "Inventory / Purchases", AccountType.ASSET, false),
          new Seed("1900", "Fixed Assets", AccountType.ASSET, false),
          new Seed("1910", "Accumulated Depreciation", AccountType.ASSET, true),
          new Seed("2000", "Accounts Payable", AccountType.LIABILITY, false),
          new Seed("2100", "Output GST Payable", AccountType.LIABILITY, false),
          new Seed("2200", "Employee Payable", AccountType.LIABILITY, false),
          new Seed("2210", "Statutory Payable (PF/ESI/PT)", AccountType.LIABILITY, false),
          new Seed("2220", "TDS Payable", AccountType.LIABILITY, false),
          new Seed("2300", "GST Payable (net)", AccountType.LIABILITY, false),
          new Seed("2400", "Loan Liability", AccountType.LIABILITY, false),
          new Seed("3000", "Owner Capital", AccountType.EQUITY, false),
          new Seed("3100", "Owner Drawings", AccountType.EQUITY, true),
          new Seed("4000", "Sales Revenue", AccountType.INCOME, false),
          new Seed("4100", "Other Income", AccountType.INCOME, false),
          new Seed("5000", "Cost of Goods Sold", AccountType.EXPENSE, false),
          new Seed("5100", "Salary & Wages Expense", AccountType.EXPENSE, false),
          new Seed("5200", "Rent / Utilities / Office Expense", AccountType.EXPENSE, false),
          new Seed("5300", "Interest Expense", AccountType.EXPENSE, false),
          new Seed("5400", "Depreciation Expense", AccountType.EXPENSE, false));

  private final ChartOfAccountRepository accounts;
  private final Clock clock;

  public CoaSeeder(ChartOfAccountRepository accounts, Clock clock) {
    this.accounts = accounts;
    this.clock = clock;
  }

  /** Seeds the standard CoA if the tenant has none. Returns the number of accounts created. */
  @Transactional
  public int seedIfAbsent(String tenantId) {
    if (accounts.existsByTenantId(tenantId)) {
      return 0;
    }
    var now = clock.instant();
    List<ChartOfAccount> toCreate =
        TEMPLATE.stream()
            .map(
                s ->
                    new ChartOfAccount(
                        Ulid.newId(), tenantId, s.code(), s.name(), s.type(), s.contra(), now))
            .toList();
    accounts.saveAll(toCreate);
    return toCreate.size();
  }
}

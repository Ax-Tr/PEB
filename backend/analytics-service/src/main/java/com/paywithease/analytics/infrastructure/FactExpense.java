package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Operating expense projection (one row per approved expense), fed from {@code EXPENSE_APPROVED}.
 */
@Entity
@Table(name = "fact_expenses")
public class FactExpense {

  @Id
  @Column(name = "expense_id", length = 26)
  private String expenseId;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column private String category;

  @Column(name = "branch_id", length = 26)
  private String branchId;

  @Column(name = "occurred_on", nullable = false)
  private LocalDate occurredOn;

  @Column(name = "period_year", nullable = false)
  private int periodYear;

  @Column(name = "period_month", nullable = false)
  private int periodMonth;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FactExpense() {}

  public FactExpense(
      String expenseId,
      String tenantId,
      String category,
      LocalDate occurredOn,
      long amountMinor,
      Instant now) {
    this.expenseId = expenseId;
    this.tenantId = tenantId;
    this.category = category;
    this.occurredOn = occurredOn;
    this.periodYear = occurredOn.getYear();
    this.periodMonth = occurredOn.getMonthValue();
    this.amountMinor = amountMinor;
    this.createdAt = now;
  }

  public String getExpenseId() {
    return expenseId;
  }

  public long getAmountMinor() {
    return amountMinor;
  }
}

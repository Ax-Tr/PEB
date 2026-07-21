package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cashflow projection (one row per confirmed cash movement). Inflows come from {@code
 * PAYMENT_RECEIVED}; outflows from {@code VENDOR_PAYMENT_COMPLETED}.
 */
@Entity
@Table(name = "fact_cash_movements")
public class FactCashMovement {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "movement_id", length = 26, columnDefinition = "char(26)")
  private String movementId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String direction; // INFLOW | OUTFLOW

  @Column(nullable = false)
  private String source; // PAYMENT | PAYOUT

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "counterparty_id", length = 26, columnDefinition = "char(26)")
  private String counterpartyId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "branch_id", length = 26, columnDefinition = "char(26)")
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

  protected FactCashMovement() {}

  public FactCashMovement(
      String movementId,
      String tenantId,
      String direction,
      String source,
      String counterpartyId,
      LocalDate occurredOn,
      long amountMinor,
      Instant now) {
    this.movementId = movementId;
    this.tenantId = tenantId;
    this.direction = direction;
    this.source = source;
    this.counterpartyId = counterpartyId;
    this.occurredOn = occurredOn;
    this.periodYear = occurredOn.getYear();
    this.periodMonth = occurredOn.getMonthValue();
    this.amountMinor = amountMinor;
    this.createdAt = now;
  }

  public String getMovementId() {
    return movementId;
  }

  public String getDirection() {
    return direction;
  }

  public long getAmountMinor() {
    return amountMinor;
  }
}

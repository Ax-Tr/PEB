package com.paywithease.cacollaboration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Collection;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Month-end close checklist. Its purpose is to gate the month lock: a period must not be locked
 * until every <b>mandatory</b> item is complete (and there is at least one mandatory item).
 */
@Entity
@Table(name = "close_checklists")
public class CloseChecklist {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "period_year", nullable = false)
  private int periodYear;

  @Column(name = "period_month", nullable = false)
  private int periodMonth;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by", length = 26, nullable = false, columnDefinition = "char(26)")
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected CloseChecklist() {}

  public CloseChecklist(
      String id, String tenantId, int periodYear, int periodMonth, String createdBy, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.periodYear = periodYear;
    this.periodMonth = periodMonth;
    this.createdBy = createdBy;
    this.createdAt = now;
  }

  /**
   * The month may be locked only when there is at least one mandatory item and every mandatory item
   * is done. Non-mandatory items never block the lock.
   */
  public static boolean canLockMonth(Collection<CloseChecklistItem> items) {
    boolean anyMandatory = items.stream().anyMatch(CloseChecklistItem::isMandatory);
    boolean allMandatoryDone =
        items.stream().filter(CloseChecklistItem::isMandatory).allMatch(CloseChecklistItem::isDone);
    return anyMandatory && allMandatoryDone;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public int getPeriodYear() {
    return periodYear;
  }

  public int getPeriodMonth() {
    return periodMonth;
  }
}

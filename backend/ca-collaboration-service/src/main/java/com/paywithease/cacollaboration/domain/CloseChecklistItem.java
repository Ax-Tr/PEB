package com.paywithease.cacollaboration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A single line of a month-end close checklist. */
@Entity
@Table(name = "close_checklist_items")
public class CloseChecklistItem {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "checklist_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String checklistId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private boolean mandatory;

  @Column(nullable = false)
  private boolean done;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "done_by", length = 26, columnDefinition = "char(26)")
  private String doneBy;

  @Column(name = "done_at")
  private Instant doneAt;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected CloseChecklistItem() {}

  public CloseChecklistItem(
      String id,
      String checklistId,
      String tenantId,
      String label,
      boolean mandatory,
      int sortOrder) {
    this.id = id;
    this.checklistId = checklistId;
    this.tenantId = tenantId;
    this.label = label;
    this.mandatory = mandatory;
    this.done = false;
    this.sortOrder = sortOrder;
  }

  public void setDone(boolean done, String actor, Instant now) {
    this.done = done;
    this.doneBy = done ? actor : null;
    this.doneAt = done ? now : null;
  }

  public String getId() {
    return id;
  }

  public String getChecklistId() {
    return checklistId;
  }

  public String getLabel() {
    return label;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public boolean isDone() {
    return done;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}

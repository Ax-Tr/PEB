package com.paywithease.commitment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Immutable lifecycle event for a commitment. */
@Entity
@Table(name = "commitment_events")
public class CommitmentEvent {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "commitment_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String commitmentId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "old_due_date")
  private LocalDate oldDueDate;

  @Column(name = "new_due_date")
  private LocalDate newDueDate;

  @Column(name = "amount_minor")
  private Long amountMinor;

  @Column private String note;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "actor_id", length = 26, columnDefinition = "char(26)")
  private String actorId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected CommitmentEvent() {}

  public CommitmentEvent(
      String id,
      String tenantId,
      String commitmentId,
      String eventType,
      LocalDate oldDueDate,
      LocalDate newDueDate,
      Long amountMinor,
      String note,
      String actorId,
      Instant occurredAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.commitmentId = commitmentId;
    this.eventType = eventType;
    this.oldDueDate = oldDueDate;
    this.newDueDate = newDueDate;
    this.amountMinor = amountMinor;
    this.note = note;
    this.actorId = actorId;
    this.occurredAt = occurredAt;
  }

  public String getId() {
    return id;
  }

  public String getEventType() {
    return eventType;
  }

  public LocalDate getOldDueDate() {
    return oldDueDate;
  }

  public LocalDate getNewDueDate() {
    return newDueDate;
  }

  public Long getAmountMinor() {
    return amountMinor;
  }

  public String getNote() {
    return note;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}

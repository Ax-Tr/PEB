package com.paywithease.commitment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Immutable lifecycle event for a commitment. */
@Entity
@Table(name = "commitment_events")
public class CommitmentEvent {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "commitment_id", length = 26, nullable = false)
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

  @Column(name = "actor_id", length = 26)
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

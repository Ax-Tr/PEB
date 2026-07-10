package com.paywithease.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit event. The application DB role has no UPDATE/DELETE on this table (ADR-0006):
 * every edit anywhere in the system leaves an immutable trail here, written in the same transaction
 * as the change and mirrored to the Audit &amp; Evidence Service via the outbox.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

  @Id
  @Column(columnDefinition = "char(26)", nullable = false)
  private String id;

  @Column(name = "tenant_id", columnDefinition = "char(26)", nullable = false)
  private String tenantId;

  @Column(name = "actor_id", columnDefinition = "char(26)")
  private String actorId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "entity_type")
  private String entityType;

  @Column(name = "entity_id", columnDefinition = "char(26)")
  private String entityId;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  private String data;

  protected AuditEvent() {}

  public AuditEvent(
      String id,
      String tenantId,
      String actorId,
      String eventType,
      String entityType,
      String entityId,
      String correlationId,
      Instant occurredAt,
      String data) {
    this.id = id;
    this.tenantId = tenantId;
    this.actorId = actorId;
    this.eventType = eventType;
    this.entityType = entityType;
    this.entityId = entityId;
    this.correlationId = correlationId;
    this.occurredAt = occurredAt;
    this.data = data;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getActorId() {
    return actorId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getData() {
    return data;
  }
}

package com.paywithease.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A per-tenant message template for a channel, with {@code {{placeholder}}} body. */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String channel;

  private String subject;

  @Column(nullable = false)
  private String body;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected NotificationTemplate() {}

  public NotificationTemplate(
      String id,
      String tenantId,
      String code,
      Channel channel,
      String subject,
      String body,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.code = code;
    this.channel = channel.name();
    this.subject = subject;
    this.body = body;
    this.active = true;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void update(String subject, String body, boolean active, Instant now) {
    this.subject = subject;
    this.body = body;
    this.active = active;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getChannel() {
    return channel;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public boolean isActive() {
    return active;
  }
}

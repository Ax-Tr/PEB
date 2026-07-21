package com.paywithease.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A scheduled reminder to fire on {@code sendOn} (a D-3/D-1/D-day offset from a due date). */
@Entity
@Table(name = "reminder_schedules")
public class ReminderSchedule {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "source_type")
  private String sourceType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "source_ref", length = 26, columnDefinition = "char(26)")
  private String sourceRef;

  @Column(name = "emi_number")
  private Integer emiNumber;

  @Column(nullable = false)
  private String channel;

  @Column(name = "template_code", nullable = false)
  private String templateCode;

  @Column(nullable = false)
  private String recipient;

  @Column(columnDefinition = "jsonb", nullable = false)
  private String variables;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "send_on", nullable = false)
  private LocalDate sendOn;

  @Column(name = "offset_days", nullable = false)
  private int offsetDays;

  @Column(nullable = false)
  private String status; // SCHEDULED, SENT, CANCELLED

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected ReminderSchedule() {}

  public ReminderSchedule(
      String id,
      String tenantId,
      String sourceType,
      String sourceRef,
      Integer emiNumber,
      Channel channel,
      String templateCode,
      String recipient,
      String variablesJson,
      LocalDate dueDate,
      LocalDate sendOn,
      int offsetDays,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.sourceType = sourceType;
    this.sourceRef = sourceRef;
    this.emiNumber = emiNumber;
    this.channel = channel.name();
    this.templateCode = templateCode;
    this.recipient = recipient;
    this.variables = variablesJson;
    this.dueDate = dueDate;
    this.sendOn = sendOn;
    this.offsetDays = offsetDays;
    this.status = "SCHEDULED";
    this.createdAt = now;
  }

  public void markSent(Instant now) {
    this.status = "SENT";
    this.sentAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSourceType() {
    return sourceType;
  }

  public Channel channelEnum() {
    return Channel.valueOf(channel);
  }

  public String getChannel() {
    return channel;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getVariables() {
    return variables;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public LocalDate getSendOn() {
    return sendOn;
  }

  public int getOffsetDays() {
    return offsetDays;
  }

  public String getStatus() {
    return status;
  }

  public Integer getEmiNumber() {
    return emiNumber;
  }

  public String getSourceRef() {
    return sourceRef;
  }
}

package com.paywithease.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request/response DTOs for the notification, template, reminder, and webhook APIs. */
public final class NotificationDtos {

  private NotificationDtos() {}

  public record CreateTemplateRequest(
      @NotBlank String code, @NotBlank String channel, String subject, @NotBlank String body) {}

  public record TemplateResponse(
      String id, String code, String channel, String subject, String body, boolean active) {}

  public record SendRequest(
      @NotBlank String channel,
      @NotBlank String templateCode,
      @NotBlank String recipient,
      Map<String, String> variables) {}

  public record ScheduleReminderRequest(
      String sourceType,
      String sourceRef,
      Integer emiNumber,
      @NotBlank String channel,
      @NotBlank String templateCode,
      @NotBlank String recipient,
      Map<String, String> variables,
      @NotNull LocalDate dueDate,
      List<Integer> offsets) {}

  public record NotificationLogResponse(
      String id,
      String channel,
      String status,
      String provider,
      String providerRef,
      int attempts) {}

  public record ReminderResponse(String id, LocalDate sendOn, int offsetDays, String status) {}

  public record DeliveryReceipt(@NotBlank String providerRef, boolean delivered, String reason) {}

  public record CountResponse(int count) {}
}

package com.paywithease.notification.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.application.NotificationService;
import com.paywithease.notification.domain.Channel;
import com.paywithease.notification.domain.NotificationLog;
import com.paywithease.notification.infrastructure.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sends templated notifications and lists recent delivery logs. */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "notifications", description = "Templated multi-channel notifications")
public class NotificationController {

  private final NotificationService service;
  private final NotificationLogRepository logs;

  public NotificationController(NotificationService service, NotificationLogRepository logs) {
    this.service = service;
    this.logs = logs;
  }

  @PostMapping("/send")
  @Operation(summary = "Render and send a templated notification")
  public NotificationDtos.NotificationLogResponse send(
      @Valid @RequestBody NotificationDtos.SendRequest body) {
    if (!Channel.isValid(body.channel())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown channel: " + body.channel());
    }
    NotificationLog log =
        service.sendTemplated(
            Channel.valueOf(body.channel()),
            body.templateCode(),
            body.recipient(),
            body.variables() == null ? Map.of() : body.variables(),
            null);
    return toResponse(log);
  }

  @GetMapping
  @Operation(summary = "List recent notifications for the current tenant")
  public List<NotificationDtos.NotificationLogResponse> list() {
    return logs.findTop100ByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId()).stream()
        .map(NotificationController::toResponse)
        .toList();
  }

  private static NotificationDtos.NotificationLogResponse toResponse(NotificationLog log) {
    return new NotificationDtos.NotificationLogResponse(
        log.getId(),
        log.getChannel(),
        log.getStatus(),
        log.getProvider(),
        log.getProviderRef(),
        log.getAttempts());
  }
}

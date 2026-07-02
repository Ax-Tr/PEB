package com.paywithease.notification.api;

import com.paywithease.notification.application.NotificationService;
import com.paywithease.notification.domain.NotificationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, provider-facing delivery-receipt webhook. Applies a DELIVERED/FAILED status to the
 * matching notification log. A message is only ever marked delivered on an actual provider ack.
 */
@RestController
@RequestMapping("/api/v1/webhooks/notifications")
@Tag(name = "notification-webhooks", description = "Provider delivery receipts")
public class NotificationWebhookController {

  private final NotificationService service;

  public NotificationWebhookController(NotificationService service) {
    this.service = service;
  }

  @PostMapping("/{provider}")
  @Operation(summary = "Receive a provider delivery receipt")
  public Map<String, String> receive(
      @PathVariable String provider, @Valid @RequestBody NotificationDtos.DeliveryReceipt body) {
    // TODO: verify provider signature (like payment webhooks) before trusting the receipt.
    NotificationLog log =
        service.recordDelivery(provider, body.providerRef(), body.delivered(), body.reason());
    return Map.of("id", log.getId(), "status", log.getStatus());
  }
}

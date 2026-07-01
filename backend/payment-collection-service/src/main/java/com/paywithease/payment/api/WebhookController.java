package com.paywithease.payment.api;

import com.paywithease.payment.application.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, provider-facing webhook endpoint. It is NOT behind JWT auth — instead every request is
 * HMAC-signature-verified (fail-closed) and de-duplicated on the provider event id inside the
 * service. The raw body is taken verbatim so the signature is computed over exactly what was sent.
 */
@RestController
@RequestMapping("/api/v1/webhooks/payments")
@Tag(name = "payment-webhooks", description = "Signature-verified, idempotent gateway callbacks")
public class WebhookController {

  private final PaymentService service;

  public WebhookController(PaymentService service) {
    this.service = service;
  }

  @PostMapping("/{provider}")
  @Operation(summary = "Receive a signed payment settlement webhook")
  public PaymentDtos.WebhookAck receive(
      @PathVariable String provider,
      @RequestBody String rawBody,
      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
      @RequestHeader(value = "X-Webhook-Timestamp", required = false) Long timestamp) {
    PaymentService.WebhookOutcome outcome =
        service.handleWebhook(provider, rawBody, signature, timestamp);
    return new PaymentDtos.WebhookAck(outcome.result(), outcome.requestId());
  }
}

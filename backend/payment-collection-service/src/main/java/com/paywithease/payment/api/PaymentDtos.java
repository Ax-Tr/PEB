package com.paywithease.payment.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for the payment-collection API. */
public final class PaymentDtos {

  private PaymentDtos() {}

  public record CreatePaymentRequest(
      @Positive long amountMinor,
      String customerId,
      boolean allowPartial,
      @Size(max = 140) String purpose,
      String payeeVpa,
      String payeeName) {}

  public record PaymentResponse(
      String requestId,
      String reference,
      long amountMinor,
      long amountPaidMinor,
      String status,
      String upiUri,
      String paymentLink) {}

  public record WebhookAck(String result, String requestId) {}
}

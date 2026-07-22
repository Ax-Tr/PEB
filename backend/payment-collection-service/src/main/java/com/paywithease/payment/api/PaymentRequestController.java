package com.paywithease.payment.api;

import com.paywithease.payment.application.PaymentService;
import com.paywithease.payment.domain.PaymentRequest;
import com.paywithease.payment.infrastructure.PaymentQrCodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create payment requests (dynamic UPI QR/link) and read their status. */
@RestController
@RequestMapping("/api/v1/payment-requests")
@Tag(name = "payment-requests", description = "Collect money via dynamic UPI QR / payment link")
public class PaymentRequestController {

  private final PaymentService service;
  private final PaymentQrCodeRepository qrCodes;

  public PaymentRequestController(PaymentService service, PaymentQrCodeRepository qrCodes) {
    this.service = service;
    this.qrCodes = qrCodes;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a payment request and generate a UPI QR/link (idempotent)")
  public PaymentDtos.PaymentResponse create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PaymentDtos.CreatePaymentRequest body) {
    PaymentService.CreateResult r =
        service.createRequest(
            idempotencyKey,
            new PaymentService.CreateCommand(
                body.customerId(),
                body.amountMinor(),
                body.allowPartial(),
                body.purpose(),
                body.payeeVpa(),
                body.payeeName()));
    return new PaymentDtos.PaymentResponse(
        r.requestId(), r.reference(), r.amountMinor(), 0, r.status(), r.upiUri(), r.paymentLink());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a payment request status")
  public PaymentDtos.PaymentResponse get(@PathVariable String id) {
    PaymentRequest request = service.get(id);
    var qr = qrCodes.findByPaymentRequestId(id).orElse(null);
    return new PaymentDtos.PaymentResponse(
        request.getId(),
        request.getReference(),
        request.getAmountMinor(),
        request.getAmountPaidMinor(),
        request.getStatus().name(),
        qr == null ? null : qr.getUpiUri(),
        qr == null ? null : qr.getPaymentLink());
  }

  @PostMapping("/{id}/simulate-payment")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Simulate payment received (local testing)")
  public PaymentDtos.PaymentResponse simulatePayment(@PathVariable String id) {
    service.markPaid(id);
    return get(id);
  }
}

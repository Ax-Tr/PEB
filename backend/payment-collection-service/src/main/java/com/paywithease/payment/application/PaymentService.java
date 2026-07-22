package com.paywithease.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.money.Money;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.payment.domain.PaymentQrCode;
import com.paywithease.payment.domain.PaymentRequest;
import com.paywithease.payment.domain.PaymentWebhook;
import com.paywithease.payment.infrastructure.PaymentQrCodeRepository;
import com.paywithease.payment.infrastructure.PaymentRequestRepository;
import com.paywithease.payment.infrastructure.PaymentWebhookRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment request creation (idempotent) and signature-verified, idempotent webhook confirmation.
 */
@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
  private static final String SOURCE = "payment-collection-service";

  private final PaymentRequestRepository requests;
  private final PaymentQrCodeRepository qrCodes;
  private final PaymentWebhookRepository webhooks;
  private final IdempotencyService idempotency;
  private final GatewaySignatureVerifier signatureVerifier;
  private final UpiUriGenerator upiGenerator;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  private final String defaultVpa;
  private final String defaultPayeeName;
  private final String defaultProvider;
  private final Duration expiry;

  public PaymentService(
      PaymentRequestRepository requests,
      PaymentQrCodeRepository qrCodes,
      PaymentWebhookRepository webhooks,
      IdempotencyService idempotency,
      GatewaySignatureVerifier signatureVerifier,
      UpiUriGenerator upiGenerator,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.payments.upi.default-vpa:merchant@upi}") String defaultVpa,
      @Value("${peb.payments.upi.default-name:PayWithEase Merchant}") String defaultPayeeName,
      @Value("${peb.payments.default-provider:UPI}") String defaultProvider,
      @Value("${peb.payments.request-expiry-minutes:60}") long expiryMinutes) {
    this.requests = requests;
    this.qrCodes = qrCodes;
    this.webhooks = webhooks;
    this.idempotency = idempotency;
    this.signatureVerifier = signatureVerifier;
    this.upiGenerator = upiGenerator;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.defaultVpa = defaultVpa;
    this.defaultPayeeName = defaultPayeeName;
    this.defaultProvider = defaultProvider;
    this.expiry = Duration.ofMinutes(expiryMinutes);
  }

  public record CreateCommand(
      String customerId,
      long amountMinor,
      boolean allowPartial,
      String purpose,
      String payeeVpa,
      String payeeName) {}

  public record CreateResult(
      String requestId,
      String reference,
      long amountMinor,
      String status,
      String upiUri,
      String paymentLink) {}

  /** Creates a payment request + dynamic UPI QR/link. Idempotent on the supplied key. */
  @Transactional
  public CreateResult createRequest(String idempotencyKey, CreateCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    String requestHash = idempotency.hashRequest(cmd);
    return idempotency.execute(
        tenantId,
        idempotencyKey,
        "POST /payment-requests",
        requestHash,
        CreateResult.class,
        () -> doCreate(tenantId, cmd));
  }

  private CreateResult doCreate(String tenantId, CreateCommand cmd) {
    if (cmd.amountMinor() <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be positive");
    }
    Instant now = clock.instant();
    String reference = "PEB" + Ulid.newId();
    PaymentRequest request =
        new PaymentRequest(
            Ulid.newId(),
            tenantId,
            cmd.customerId(),
            reference,
            cmd.amountMinor(),
            cmd.allowPartial(),
            cmd.purpose(),
            now.plus(expiry),
            now);
    request.markProvider(defaultProvider);
    requests.save(request);

    String vpa = blankTo(cmd.payeeVpa(), defaultVpa);
    String payeeName = blankTo(cmd.payeeName(), defaultPayeeName);
    String upiUri =
        upiGenerator.buildUpiUri(
            vpa, payeeName, Money.ofMinor(cmd.amountMinor()), cmd.purpose(), reference);
    String link = upiGenerator.buildPaymentLink(reference);
    qrCodes.save(new PaymentQrCode(Ulid.newId(), tenantId, request.getId(), upiUri, link, now));

    audit.record(
        "PAYMENT_REQUEST_CREATED",
        "payment_request",
        request.getId(),
        Map.of("amountMinor", cmd.amountMinor(), "reference", reference));
    emit("PAYMENT_REQUEST_CREATED", request, Map.of("amountMinor", cmd.amountMinor()));
    emit("PAYMENT_QR_GENERATED", request, Map.of("reference", reference));

    return new CreateResult(
        request.getId(),
        reference,
        request.getAmountMinor(),
        request.getStatus().name(),
        upiUri,
        link);
  }

  @Transactional(readOnly = true)
  public PaymentRequest get(String id) {
    PaymentRequest request =
        requests.findById(id).orElseThrow(() -> ApiException.notFound("Payment request"));
    if (!request.getTenantId().equals(TenantContext.requireTenantId())) {
      throw ApiException.notFound("Payment request");
    }
    return request;
  }

  public record WebhookOutcome(String result, String requestId) {}

  /**
   * Handles an inbound gateway webhook. Signature is verified before parsing; the effect is
   * idempotent on {@code (provider, eventId)}; the applied amount is clamped to the outstanding
   * balance. Emits PAYMENT_RECEIVED / PAYMENT_FAILED.
   */
  @Transactional
  public WebhookOutcome handleWebhook(
      String provider, String rawBody, String signatureHeader, Long timestamp) {
    if (!signatureVerifier.verify(provider, rawBody, signatureHeader)) {
      // Do not persist unverified payloads; fail closed.
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Invalid webhook signature");
    }
    if (!signatureVerifier.isFresh(timestamp)) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Stale webhook (possible replay)");
    }

    GatewayEvent event = parse(rawBody);
    if (webhooks.existsByProviderAndProviderEventId(provider, event.eventId())) {
      // Redelivery — already processed. No-op.
      return new WebhookOutcome("DUPLICATE_IGNORED", null);
    }

    PaymentRequest request =
        requests
            .findByReference(event.reference())
            .orElseThrow(() -> ApiException.notFound("Payment request for reference"));

    // Attribute audit/events to the request's tenant.
    TenantContext.set(
        new TenantContext.Principal(
            request.getTenantId(),
            request.getTenantId(),
            "gateway:" + provider,
            TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null)));

    Instant now = clock.instant();
    String result;
    if ("SUCCESS".equalsIgnoreCase(event.status())) {
      PaymentRequest.Applied applied = request.applyPayment(event.amountMinor(), now);
      requests.save(request);
      if (applied.overpaidMinor() > 0) {
        log.warn(
            "Payment overpayment on request {}: overpaid {} paise (reconciliation flagged)",
            request.getId(),
            applied.overpaidMinor());
      }
      emit(
          "PAYMENT_RECEIVED",
          request,
          Map.of(
              "appliedMinor", applied.appliedMinor(),
              "overpaidMinor", applied.overpaidMinor(),
              "amountPaidMinor", request.getAmountPaidMinor(),
              "fullyPaid", applied.fullyPaid(),
              "providerPaymentId", nullSafe(event.providerPaymentId())));
      audit.record(
          "PAYMENT_RECEIVED",
          "payment_request",
          request.getId(),
          Map.of("appliedMinor", applied.appliedMinor(), "fullyPaid", applied.fullyPaid()));
      result = applied.fullyPaid() ? "PAID" : "PARTIALLY_PAID";
    } else {
      request.markFailed(now);
      requests.save(request);
      emit("PAYMENT_FAILED", request, Map.of("reason", nullSafe(event.status())));
      audit.record("PAYMENT_FAILED", "payment_request", request.getId(), Map.of());
      result = "FAILED";
    }

    webhooks.save(
        new PaymentWebhook(
            Ulid.newId(),
            request.getTenantId(),
            provider,
            event.eventId(),
            true,
            "PROCESSED",
            event.reference(),
            rawBody,
            now));
    return new WebhookOutcome(result, request.getId());
  }

  @Transactional
  public void markPaid(String id) {
    PaymentRequest request = get(id);
    PaymentRequest.Applied applied =
        request.applyPayment(request.getAmountMinor(), clock.instant());
    requests.save(request);
    emit(
        "PAYMENT_RECEIVED",
        request,
        Map.of(
            "appliedMinor", applied.appliedMinor(),
            "overpaidMinor", applied.overpaidMinor(),
            "amountPaidMinor", request.getAmountPaidMinor(),
            "fullyPaid", applied.fullyPaid(),
            "providerPaymentId", "simulated"));
    audit.record(
        "PAYMENT_RECEIVED",
        "payment_request",
        request.getId(),
        Map.of("appliedMinor", applied.appliedMinor(), "fullyPaid", applied.fullyPaid()));
  }

  private GatewayEvent parse(String rawBody) {
    try {
      GatewayEvent event = objectMapper.readValue(rawBody, GatewayEvent.class);
      if (event.eventId() == null || event.reference() == null || event.status() == null) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "webhook missing required fields");
      }
      return event;
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "malformed webhook payload");
    }
  }

  private void emit(String eventType, PaymentRequest request, Map<String, ?> extra) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("paymentRequestId", request.getId());
    payload.put("reference", request.getReference());
    payload.put("status", request.getStatus().name());
    payload.setAll((ObjectNode) objectMapper.valueToTree(extra));
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(request.getTenantId())
            .businessId(request.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(request.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static String blankTo(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }
}

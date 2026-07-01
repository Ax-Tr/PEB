package com.paywithease.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.payment.domain.PaymentRequest;
import com.paywithease.payment.domain.PaymentStatus;
import com.paywithease.payment.infrastructure.PaymentQrCodeRepository;
import com.paywithease.payment.infrastructure.PaymentRequestRepository;
import com.paywithease.payment.infrastructure.PaymentWebhookRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock PaymentRequestRepository requests;
  @Mock PaymentQrCodeRepository qrCodes;
  @Mock PaymentWebhookRepository webhooks;
  @Mock IdempotencyService idempotency;
  @Mock GatewaySignatureVerifier signatureVerifier;
  @Mock UpiUriGenerator upiGenerator;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private PaymentService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new PaymentService(
            requests,
            qrCodes,
            webhooks,
            idempotency,
            signatureVerifier,
            upiGenerator,
            audit,
            outbox,
            objectMapper,
            clock,
            "merchant@upi",
            "Merchant",
            "UPI",
            60);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createRequestGeneratesUpiAndEmitsTwoEvents() {
    when(idempotency.hashRequest(any())).thenReturn("h");
    when(idempotency.execute(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(PaymentService.CreateResult.class),
            any()))
        .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(5)).get());
    when(requests.save(any())).thenAnswer(returnsFirstArg());
    when(upiGenerator.buildUpiUri(any(), any(), any(), any(), any())).thenReturn("upi://pay?x");
    when(upiGenerator.buildPaymentLink(any())).thenReturn("https://link");

    PaymentService.CreateResult r =
        service.createRequest(
            "idem-1", new PaymentService.CreateCommand(null, 150000, false, "Order", null, null));

    assertThat(r.amountMinor()).isEqualTo(150000);
    assertThat(r.status()).isEqualTo(PaymentStatus.AWAITING_PAYMENT.name());
    assertThat(r.upiUri()).isEqualTo("upi://pay?x");
    verify(qrCodes).save(any());
    verify(outbox, times(2)).append(any(EventEnvelope.class)); // REQUEST_CREATED + QR_GENERATED
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void webhookSuccessFullyPays() {
    stubValidWebhook();
    PaymentRequest req = awaiting("PEB-REF", 10000);
    when(requests.findByReference("PEB-REF")).thenReturn(Optional.of(req));
    when(requests.save(any())).thenAnswer(returnsFirstArg());

    String body =
        "{\"eventId\":\"e1\",\"reference\":\"PEB-REF\",\"amountMinor\":10000,\"status\":\"SUCCESS\"}";
    PaymentService.WebhookOutcome outcome = service.handleWebhook("UPI", body, "sig", null);

    assertThat(outcome.result()).isEqualTo("PAID");
    assertThat(req.getStatus()).isEqualTo(PaymentStatus.PAID);
    verify(outbox).append(any(EventEnvelope.class)); // PAYMENT_RECEIVED
    verify(webhooks).save(any());
  }

  @Test
  void webhookPartialPayment() {
    stubValidWebhook();
    PaymentRequest req = awaiting("PEB-REF", 10000);
    when(requests.findByReference("PEB-REF")).thenReturn(Optional.of(req));
    when(requests.save(any())).thenAnswer(returnsFirstArg());

    String body =
        "{\"eventId\":\"e2\",\"reference\":\"PEB-REF\",\"amountMinor\":4000,\"status\":\"SUCCESS\"}";
    PaymentService.WebhookOutcome outcome = service.handleWebhook("UPI", body, "sig", null);

    assertThat(outcome.result()).isEqualTo("PARTIALLY_PAID");
    assertThat(req.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(req.getAmountPaidMinor()).isEqualTo(4000);
  }

  @Test
  void webhookOverpaymentIsClampedToPaid() {
    stubValidWebhook();
    PaymentRequest req = awaiting("PEB-REF", 10000);
    when(requests.findByReference("PEB-REF")).thenReturn(Optional.of(req));
    when(requests.save(any())).thenAnswer(returnsFirstArg());

    String body =
        "{\"eventId\":\"e3\",\"reference\":\"PEB-REF\",\"amountMinor\":15000,\"status\":\"SUCCESS\"}";
    PaymentService.WebhookOutcome outcome = service.handleWebhook("UPI", body, "sig", null);

    assertThat(outcome.result()).isEqualTo("PAID");
    assertThat(req.getAmountPaidMinor()).isEqualTo(10000); // clamped, never exceeds requested total
  }

  @Test
  void webhookDuplicateIsIgnored() {
    when(signatureVerifier.verify(anyString(), anyString(), any())).thenReturn(true);
    when(signatureVerifier.isFresh(nullable(Long.class))).thenReturn(true);
    when(webhooks.existsByProviderAndProviderEventId("UPI", "e1")).thenReturn(true);

    String body =
        "{\"eventId\":\"e1\",\"reference\":\"PEB-REF\",\"amountMinor\":10000,\"status\":\"SUCCESS\"}";
    PaymentService.WebhookOutcome outcome = service.handleWebhook("UPI", body, "sig", null);

    assertThat(outcome.result()).isEqualTo("DUPLICATE_IGNORED");
    verify(requests, never()).findByReference(any());
    verify(outbox, never()).append(any());
  }

  @Test
  void webhookRejectsBadSignature() {
    when(signatureVerifier.verify(anyString(), anyString(), any())).thenReturn(false);
    assertThatThrownBy(() -> service.handleWebhook("UPI", "{}", "bad", null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Invalid webhook signature");
    verify(webhooks, never()).save(any());
  }

  private void stubValidWebhook() {
    when(signatureVerifier.verify(anyString(), anyString(), any())).thenReturn(true);
    when(signatureVerifier.isFresh(nullable(Long.class))).thenReturn(true);
    when(webhooks.existsByProviderAndProviderEventId(anyString(), anyString())).thenReturn(false);
    when(webhooks.save(any())).thenAnswer(returnsFirstArg());
  }

  private PaymentRequest awaiting(String reference, long amountMinor) {
    return new PaymentRequest(
        "req1", "tenant1", "cust1", reference, amountMinor, false, "p", null, clock.instant());
  }
}

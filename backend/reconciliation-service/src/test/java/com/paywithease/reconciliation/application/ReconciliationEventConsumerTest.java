package com.paywithease.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.reconciliation.domain.ReconSide;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationEventConsumerTest {

  @Mock ReconciliationService reconciliationService;

  private ReconciliationEventConsumer consumer;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    consumer = new ReconciliationEventConsumer(reconciliationService, objectMapper, clock);
  }

  @Test
  void bankTransactionImportedRecordsExternalItem() {
    String message =
        """
        {
          "eventType": "BANK_TRANSACTION_IMPORTED",
          "tenantId": "tenant1",
          "aggregateId": "txn1",
          "payload": { "transactionId": "txn1", "direction": "CREDIT", "amountMinor": 118000, "externalRef": "UTR123", "category": "SALES" },
          "headers": { "eventId": "evt1", "correlationId": "corr1", "partitionKey": "tenant1" }
        }
        """;

    consumer.onEvent(message);

    ArgumentCaptor<ReconciliationService.ItemCommand> captor =
        ArgumentCaptor.forClass(ReconciliationService.ItemCommand.class);
    verify(reconciliationService, times(1)).recordItem(eq(ReconSide.EXTERNAL), captor.capture());
    assertThat(captor.getValue().sourceType()).isEqualTo("BANK_TXN");
    assertThat(captor.getValue().sourceRef()).isEqualTo("txn1");
    assertThat(captor.getValue().amountMinor()).isEqualTo(118000);
  }

  @Test
  void paymentReceivedRecordsInternalItem() {
    String message =
        """
        {
          "eventType": "PAYMENT_RECEIVED",
          "tenantId": "tenant1",
          "aggregateId": "pay1",
          "payload": { "paymentRequestId": "pay1", "appliedMinor": 118000, "reference": "PEB-REF" },
          "headers": { "eventId": "evt2", "correlationId": "corr2", "partitionKey": "tenant1" }
        }
        """;

    consumer.onEvent(message);

    ArgumentCaptor<ReconciliationService.ItemCommand> captor =
        ArgumentCaptor.forClass(ReconciliationService.ItemCommand.class);
    verify(reconciliationService, times(1)).recordItem(eq(ReconSide.INTERNAL), captor.capture());
    assertThat(captor.getValue().sourceType()).isEqualTo("PAYMENT");
    assertThat(captor.getValue().direction()).isEqualTo("CREDIT");
  }
}

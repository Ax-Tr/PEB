package com.paywithease.analytics.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventConsumerTest {

  @Mock AnalyticsService service;

  private AnalyticsEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer =
        new AnalyticsEventConsumer(
            service,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void projectsCommitmentEventsAndAdvancesWatermark() {
    consumer.onEvent(
        """
        {
          "eventType":"COMMITMENT_CREATED",
          "tenantId":"tenant1",
          "headers":{"eventId":"evt1","correlationId":"corr1"},
          "payload":{
            "commitmentId":"c1",
            "counterpartyType":"CUSTOMER",
            "counterpartyId":"cust1",
            "counterpartyName":"Raj",
            "sourceType":"VOICE",
            "dueDate":"2026-07-10",
            "amountMinor":500000,
            "paidMinor":0,
            "outstandingMinor":500000,
            "status":"PROMISED"
          }
        }
        """);

    verify(service)
        .ingestCommitment(
            eq("c1"),
            eq("CUSTOMER"),
            eq("cust1"),
            eq("Raj"),
            eq("VOICE"),
            eq(LocalDate.of(2026, 7, 10)),
            eq(500000L),
            eq(0L),
            eq(500000L),
            eq("PROMISED"));
    verify(service).advanceWatermark("commitment.events", "evt1");
  }

  @Test
  void ignoresCommitmentEventsMissingRequiredFields() {
    consumer.onEvent(
        """
        {
          "eventType":"COMMITMENT_CREATED",
          "tenantId":"tenant1",
          "headers":{"eventId":"evt1"},
          "payload":{"counterpartyType":"CUSTOMER"}
        }
        """);

    verify(service, never())
        .ingestCommitment(
            any(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyLong(), any());
    verify(service, never()).advanceWatermark(eq("commitment.events"), any());
  }

  @Test
  void poisonEventIsSwallowedAndTenantContextIsCleared() {
    consumer.onEvent("{not-json");

    verify(service, never()).advanceWatermark(any(), any());
    org.assertj.core.api.Assertions.assertThat(TenantContext.current()).isEmpty();
  }
}

package com.paywithease.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.ledger.domain.JournalCommand;
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
class LedgerEventConsumerTest {

  @Mock CoaSeeder coaSeeder;
  @Mock LedgerPostingService postingService;

  private LedgerEventConsumer consumer;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    consumer = new LedgerEventConsumer(coaSeeder, postingService, objectMapper, clock);
  }

  @Test
  void businessCreatedSeedsChartOfAccounts() {
    String message =
        """
        {
          "eventType": "BUSINESS_CREATED",
          "tenantId": "tenant1",
          "aggregateId": "tenant1",
          "payload": {},
          "headers": { "eventId": "evt0", "correlationId": "corr0", "partitionKey": "tenant1" }
        }
        """;

    consumer.onEvent(message);

    verify(coaSeeder).seedIfAbsent("tenant1");
  }

  @Test
  void invoiceGeneratedPostsBalancedJournal() {
    String message =
        """
        {
          "eventType": "INVOICE_GENERATED",
          "tenantId": "tenant1",
          "aggregateId": "inv1",
          "payload": { "totalAmountMinor": 118000, "totalTaxMinor": 18000, "invoiceNumber": "INV/2026-27/00001" },
          "headers": { "eventId": "evt1", "correlationId": "corr1", "partitionKey": "tenant1" }
        }
        """;

    consumer.onEvent(message);

    ArgumentCaptor<JournalCommand> captor = ArgumentCaptor.forClass(JournalCommand.class);
    verify(postingService, times(1)).post(captor.capture(), any());
    JournalCommand cmd = captor.getValue();
    long debit = cmd.lines().stream().mapToLong(JournalCommand.Line::debitMinor).sum();
    long credit = cmd.lines().stream().mapToLong(JournalCommand.Line::creditMinor).sum();
    assertThat(debit).isEqualTo(credit);
    assertThat(cmd.sourceEventId()).isEqualTo("evt1");
  }

  @Test
  void paymentReceivedPostsBalancedJournal() {
    String message =
        """
        {
          "eventType": "PAYMENT_RECEIVED",
          "tenantId": "tenant1",
          "aggregateId": "pay1",
          "payload": { "appliedMinor": 118000, "reference": "PEB-REF" },
          "headers": { "eventId": "evt2", "correlationId": "corr2", "partitionKey": "tenant1" }
        }
        """;

    consumer.onEvent(message);

    ArgumentCaptor<JournalCommand> captor = ArgumentCaptor.forClass(JournalCommand.class);
    verify(postingService, times(1)).post(captor.capture(), any());
    JournalCommand cmd = captor.getValue();
    long debit = cmd.lines().stream().mapToLong(JournalCommand.Line::debitMinor).sum();
    long credit = cmd.lines().stream().mapToLong(JournalCommand.Line::creditMinor).sum();
    assertThat(debit).isEqualTo(credit);
    assertThat(cmd.sourceEventId()).isEqualTo("evt2");
  }
}

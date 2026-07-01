package com.paywithease.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

  @Mock OutboxRepository repository;
  @Mock KafkaTemplate<String, String> kafka;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void publishesUnpublishedEventToResolvedTopicAndMarksPublished() {
    OutboxRelay relay =
        new OutboxRelay(
            repository, kafka, TopicResolver.defaultResolver(), objectMapper, clock, 100);
    OutboxEvent event =
        new OutboxEvent(
            "01J0000000000000000000EVT1",
            "invoice",
            "01J0000000000000000000AGG1",
            "INVOICE_GENERATED",
            1,
            "01J0000000000000000000TEN1",
            "{\"invoiceId\":\"x\"}",
            "{\"partitionKey\":\"t:a\"}",
            clock.instant());
    when(repository.claimUnpublished(any(Limit.class))).thenReturn(List.of(event));
    doReturn(CompletableFuture.completedFuture(null)).when(kafka).send(any(), any(), any());

    relay.publishBatch();

    verify(kafka).send(eq("invoice.events"), eq("t:a"), any());
    assertThat(event.getPublishedAt()).isEqualTo(clock.instant());
    verify(repository).save(event);
  }
}

package com.paywithease.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a domain event to the outbox in the caller's transaction. Call this from application
 * services immediately after the state mutation so the event and the state commit atomically.
 */
@Component
public class OutboxWriter {

  private final OutboxRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public OutboxWriter(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String append(EventEnvelope envelope) {
    try {
      String payload = objectMapper.writeValueAsString(envelope.payload());
      String headers =
          objectMapper.writeValueAsString(
              Map.of(
                  "eventId", envelope.eventId(),
                  "correlationId", nullSafe(envelope.correlationId()),
                  "causationId", nullSafe(envelope.causationId()),
                  "partitionKey", envelope.partitionKey()));
      String partitionKey = envelope.partitionKey();
      String aggregateId = partitionKey;
      if (partitionKey != null) {
        int idx = partitionKey.indexOf(':');
        if (idx >= 0) {
          aggregateId = partitionKey.substring(idx + 1);
        }
      }
      OutboxEvent event =
          new OutboxEvent(
              Ulid.newId(),
              envelope.eventType(),
              aggregateId,
              envelope.eventType(),
              envelope.eventVersion(),
              envelope.tenantId() != null ? envelope.tenantId() : "00000000000000000000000000",
              payload,
              headers,
              clock.instant());
      repository.save(event);
      return event.getId();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize event payload", e);
    }
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }
}

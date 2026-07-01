package com.paywithease.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polling transactional-outbox relay: claims unpublished rows with {@code FOR UPDATE SKIP LOCKED}
 * (so multiple instances share the work safely), publishes each to Kafka keyed by partition key for
 * per-aggregate ordering, then marks them published. At-least-once delivery — consumers dedupe on
 * {@code eventId}. Enabled per service via {@code peb.outbox.relay.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "peb.outbox.relay.enabled", havingValue = "true")
public class OutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

  private final OutboxRepository repository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final TopicResolver topicResolver;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final int batchSize;

  public OutboxRelay(
      OutboxRepository repository,
      KafkaTemplate<String, String> kafkaTemplate,
      TopicResolver topicResolver,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.outbox.relay.batch-size:100}") int batchSize) {
    this.repository = repository;
    this.kafkaTemplate = kafkaTemplate;
    this.topicResolver = topicResolver;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${peb.outbox.relay.poll-ms:1000}")
  @Transactional
  public void publishBatch() {
    List<OutboxEvent> batch = repository.claimUnpublished(Limit.of(batchSize));
    for (OutboxEvent event : batch) {
      try {
        String topic = topicResolver.topicFor(event.getEventType());
        String key = partitionKey(event);
        kafkaTemplate.send(topic, key, toMessage(event)).get();
        event.markPublished(clock.instant());
        repository.save(event);
      } catch (Exception e) {
        event.incrementAttempts();
        repository.save(event);
        log.warn(
            "Outbox publish failed for event {} (attempt {})",
            event.getId(),
            event.getAttempts(),
            e);
      }
    }
  }

  private String toMessage(OutboxEvent event) {
    try {
      ObjectNode node = objectMapper.createObjectNode();
      node.put("eventType", event.getEventType());
      node.put("eventVersion", event.getEventVersion());
      node.put("tenantId", event.getTenantId());
      node.put("aggregateType", event.getAggregateType());
      node.put("aggregateId", event.getAggregateId());
      node.set("payload", objectMapper.readTree(event.getPayload()));
      node.set("headers", objectMapper.readTree(event.getHeaders()));
      return objectMapper.writeValueAsString(node);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize outbox message " + event.getId(), e);
    }
  }

  private String partitionKey(OutboxEvent event) {
    try {
      var headers = objectMapper.readTree(event.getHeaders());
      String pk = headers.path("partitionKey").asText(null);
      return pk != null ? pk : event.getTenantId() + ":" + event.getAggregateId();
    } catch (Exception e) {
      return event.getTenantId() + ":" + event.getAggregateId();
    }
  }
}

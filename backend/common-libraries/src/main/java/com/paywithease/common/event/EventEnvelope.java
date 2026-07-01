package com.paywithease.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.paywithease.common.ids.Ulid;
import java.time.Instant;

/**
 * Mandatory envelope on every domain event (Data/API/Event blueprint). Carries provenance,
 * multi-tenancy, causality, and schema version. {@code partitionKey} = {@code tenantId:aggregateId}
 * so Kafka preserves per-aggregate ordering.
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    int eventVersion,
    String tenantId,
    String businessId,
    String sourceService,
    String actorId,
    String correlationId,
    String causationId,
    Instant occurredAt,
    String partitionKey,
    JsonNode payload) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String eventType;
    private int eventVersion = 1;
    private String tenantId;
    private String businessId;
    private String sourceService;
    private String actorId;
    private String correlationId;
    private String causationId;
    private String aggregateId;
    private JsonNode payload;

    public Builder eventType(String v) {
      this.eventType = v;
      return this;
    }

    public Builder eventVersion(int v) {
      this.eventVersion = v;
      return this;
    }

    public Builder tenantId(String v) {
      this.tenantId = v;
      return this;
    }

    public Builder businessId(String v) {
      this.businessId = v;
      return this;
    }

    public Builder sourceService(String v) {
      this.sourceService = v;
      return this;
    }

    public Builder actorId(String v) {
      this.actorId = v;
      return this;
    }

    public Builder correlationId(String v) {
      this.correlationId = v;
      return this;
    }

    public Builder causationId(String v) {
      this.causationId = v;
      return this;
    }

    public Builder aggregateId(String v) {
      this.aggregateId = v;
      return this;
    }

    public Builder payload(JsonNode v) {
      this.payload = v;
      return this;
    }

    public EventEnvelope build(Instant occurredAt) {
      String id = Ulid.newId();
      String partitionKey = tenantId + ":" + aggregateId;
      return new EventEnvelope(
          id,
          eventType,
          eventVersion,
          tenantId,
          businessId,
          sourceService,
          actorId,
          correlationId,
          causationId,
          occurredAt,
          partitionKey,
          payload);
    }
  }
}

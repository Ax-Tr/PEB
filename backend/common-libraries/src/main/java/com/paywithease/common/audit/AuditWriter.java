package com.paywithease.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes an immutable audit row in the caller's transaction. */
@Component
public class AuditWriter {

  private final AuditRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AuditWriter(AuditRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String record(String eventType, String entityType, String entityId, Map<String, ?> data) {
    var principal = TenantContext.current().orElse(null);
    String tenantId =
        (principal != null && principal.tenantId() != null)
            ? principal.tenantId()
            : "00000000000000000000000000";
    String actorId = principal != null ? principal.actorId() : null;
    String correlationId = principal != null ? principal.correlationId() : null;
    try {
      AuditEvent event =
          new AuditEvent(
              Ulid.newId(),
              tenantId,
              actorId,
              eventType,
              entityType,
              entityId,
              correlationId,
              clock.instant(),
              objectMapper.writeValueAsString(data == null ? Map.of() : data));
      repository.save(event);
      return event.getId();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize audit data", e);
    }
  }
}

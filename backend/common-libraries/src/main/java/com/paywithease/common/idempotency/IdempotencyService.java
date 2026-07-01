package com.paywithease.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Durable idempotency for money/ledger mutations (specs/idempotency-outbox-saga.md). Runs an action
 * at most once per {@code (tenantId, key)}: a replay with the same request hash returns the stored
 * response; a replay with a different body is rejected; a concurrent in-flight duplicate is a
 * conflict. The idempotency row commits in the caller's transaction alongside the action, so a
 * failed action rolls the key back and a later retry re-runs cleanly.
 */
@Service
public class IdempotencyService {

  private final IdempotencyKeyRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public IdempotencyService(
      IdempotencyKeyRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Deterministic hash of the request body used to detect key reuse with a different payload. */
  public String hashRequest(Object request) {
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(request);
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(bytes));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to hash idempotent request", e);
    }
  }

  public <T> T execute(
      String tenantId,
      String key,
      String endpoint,
      String requestHash,
      Class<T> responseType,
      Supplier<T> action) {
    if (key == null || key.isBlank()) {
      // No idempotency key supplied: execute without dedupe (caller decides if key is mandatory).
      return action.get();
    }

    Optional<IdempotencyKey> existing = repository.findByTenantIdAndKey(tenantId, key);
    if (existing.isPresent()) {
      return replay(existing.get(), requestHash, responseType);
    }

    IdempotencyKey record =
        new IdempotencyKey(tenantId, key, endpoint, requestHash, clock.instant());
    try {
      repository.saveAndFlush(record);
    } catch (DataIntegrityViolationException concurrent) {
      // Another request inserted the same key first; treat as a replay.
      IdempotencyKey other =
          repository
              .findByTenantIdAndKey(tenantId, key)
              .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "Idempotency race"));
      return replay(other, requestHash, responseType);
    }

    T result = action.get();
    record.complete(serialize(result));
    repository.save(record);
    return result;
  }

  private <T> T replay(IdempotencyKey record, String requestHash, Class<T> responseType) {
    if (!record.getRequestHash().equals(requestHash)) {
      throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
    }
    if (record.getStatus() == IdempotencyKey.Status.COMPLETED && record.getResponse() != null) {
      return deserialize(record.getResponse(), responseType);
    }
    // Still PROCESSING (or a prior failure): the client should retry shortly.
    throw new ApiException(
        ErrorCode.CONFLICT, "A request with this idempotency key is in progress");
  }

  private String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to serialize idempotent response", e);
    }
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to deserialize idempotent response", e);
    }
  }
}

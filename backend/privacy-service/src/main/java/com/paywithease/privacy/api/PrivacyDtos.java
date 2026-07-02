package com.paywithease.privacy.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.privacy.domain.DataCategory;
import com.paywithease.privacy.domain.DsrRequest;
import com.paywithease.privacy.domain.DsrType;
import com.paywithease.privacy.domain.ErasurePlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Request/response DTOs for the /privacy API. */
public final class PrivacyDtos {

  private PrivacyDtos() {}

  // -------------------- Requests --------------------

  /**
   * Intake of a data-principal request. {@code type} must be a valid {@link DsrType}. The subject's
   * email is PII — encrypted at rest and never logged (logs mask PII via the logback converter).
   */
  public record SubmitRequest(
      @NotBlank String type, String subjectRef, @NotBlank String subjectEmail, String details) {}

  /** Categories to include in the erasure plan; each must be a valid {@link DataCategory}. */
  public record ErasurePlanRequest(@NotNull List<String> categories) {}

  /** Record completion evidence for a resolved request. */
  public record CompleteRequest(@NotBlank String evidenceRef, String note) {}

  /** Reject a request with a reason. */
  public record RejectRequest(@NotBlank String reason) {}

  // -------------------- Responses --------------------

  /**
   * DSR view returned to authorised staff (DPO function). {@code subjectEmail} is decrypted PII —
   * returned here only because these endpoints are owner/DPO-guarded — and must never be logged.
   */
  public record DsrResponse(
      String id,
      String type,
      String status,
      String subjectRef,
      String subjectEmail,
      String details,
      JsonNode erasurePlan,
      String resolutionNote,
      String evidenceRef,
      java.time.Instant dueAt) {}

  public record ErasurePlanLineResponse(
      String category, String action, int minRetentionYears, String reason) {}

  public record ErasurePlanResponse(
      List<ErasurePlanLineResponse> lines, boolean fullErasurePossible, String summary) {}

  // -------------------- Mappers / validation --------------------

  static DsrResponse toDsr(DsrRequest r, ObjectMapper objectMapper) {
    return new DsrResponse(
        r.getId(),
        r.getType(),
        r.getStatus(),
        r.getSubjectRef(),
        r.getSubjectEmail(),
        r.getDetails(),
        parseErasurePlan(r.getErasurePlan(), objectMapper),
        r.getResolutionNote(),
        r.getEvidenceRef(),
        r.getDueAt());
  }

  static ErasurePlanResponse toPlan(ErasurePlan.Plan plan) {
    List<ErasurePlanLineResponse> lines =
        plan.lines().stream()
            .map(
                l ->
                    new ErasurePlanLineResponse(
                        l.category().name(), l.action().name(), l.minRetentionYears(), l.reason()))
            .toList();
    return new ErasurePlanResponse(lines, plan.fullErasurePossible(), plan.summary());
  }

  /** Maps the request's {@code type} string to a {@link DsrType}. */
  static DsrType parseType(String type) {
    if (type == null || !DsrType.isValid(type)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown DSR type: " + type);
    }
    return DsrType.valueOf(type);
  }

  /** Maps the request's category strings to {@link DataCategory} values, rejecting unknowns. */
  static List<DataCategory> parseCategories(List<String> categories) {
    if (categories == null || categories.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "At least one data category is required");
    }
    return categories.stream()
        .map(
            c -> {
              try {
                return DataCategory.valueOf(c);
              } catch (IllegalArgumentException | NullPointerException e) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown data category: " + c);
              }
            })
        .toList();
  }

  /** Parses the stored erasure-plan JSON into a JsonNode; falls back to a raw text node. */
  private static JsonNode parseErasurePlan(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      return objectMapper.getNodeFactory().textNode(json);
    }
  }
}

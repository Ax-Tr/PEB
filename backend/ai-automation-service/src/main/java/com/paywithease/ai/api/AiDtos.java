package com.paywithease.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.paywithease.ai.application.AiAutomationService;
import com.paywithease.ai.domain.AnomalyDetector;
import com.paywithease.ai.infrastructure.AiFeedback;
import com.paywithease.ai.infrastructure.AiSuggestion;
import com.paywithease.ai.infrastructure.AnomalyAlert;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request/response DTOs for the /ai API. Every response that carries an AI output also carries its
 * confidence/score so the UI can always surface it (AC: every AI output shows confidence).
 * BigDecimal confidence/score serialise as JSON numbers and enums serialise via {@code .name()}.
 * The stored suggestion payload (a JSON string) is parsed back into a JsonNode so the response
 * nests an object rather than an escaped string.
 */
public final class AiDtos {

  private AiDtos() {}

  // -------------------- Suggestion requests --------------------

  /** Classify a bank transaction narration into a category as a governed suggestion. */
  public record ClassifyTransactionRequest(
      @NotBlank String subjectId, @NotBlank String narration) {}

  /**
   * Record an OCR bank-detail extraction as a suggestion. Never auto-applied — the user must
   * review. Confidence must be in [0,1].
   */
  public record BankDetailRequest(
      @NotBlank String subjectId,
      @NotNull Map<String, String> fields,
      @DecimalMin("0.0") @DecimalMax("1.0") double confidence) {}

  /** Advisory next-period cashflow forecast over a series of period net amounts (paise). */
  public record CashflowForecastRequest(@NotNull List<Long> periodNets) {}

  /** Human feedback on a suggestion, keeping the review loop auditable. */
  public record FeedbackRequest(@NotNull Boolean helpful, String note) {}

  // -------------------- Anomaly request --------------------

  /** Evaluate an observed metric value against its history; raises an alert only if anomalous. */
  public record DetectAnomalyRequest(
      @NotBlank String subjectType,
      @NotBlank String subjectId,
      @NotBlank String metric,
      @NotNull List<Long> history,
      long observed) {}

  // -------------------- Assistant request --------------------

  /** A natural-language finance question for the advisory assistant. */
  public record AskAssistantRequest(@NotBlank String question) {}

  // -------------------- Responses --------------------

  public record SuggestionResponse(
      String id,
      String kind,
      String subjectType,
      String subjectId,
      JsonNode suggestion,
      BigDecimal confidence,
      String decision,
      String status,
      String modelRef) {

    static SuggestionResponse from(AiSuggestion s, ObjectMapper objectMapper) {
      return new SuggestionResponse(
          s.getId(),
          s.getKind(),
          s.getSubjectType(),
          s.getSubjectId(),
          parseSuggestion(s.getSuggestion(), objectMapper),
          s.getConfidence(),
          s.getDecision(),
          s.getStatus(),
          s.getModelRef());
    }
  }

  public record FeedbackResponse(String id, String suggestionId, boolean helpful) {

    static FeedbackResponse from(AiFeedback f) {
      return new FeedbackResponse(f.getId(), f.getSuggestionId(), f.isHelpful());
    }
  }

  public record AnomalyResultResponse(
      boolean anomaly, double score, String severity, String detail) {

    static AnomalyResultResponse from(AnomalyDetector.Result r) {
      return new AnomalyResultResponse(r.anomaly(), r.score(), r.severity().name(), r.detail());
    }
  }

  public record AnomalyAlertResponse(
      String id,
      String subjectType,
      String subjectId,
      long observedMinor,
      BigDecimal score,
      String severity,
      String status,
      String detail) {

    static AnomalyAlertResponse from(AnomalyAlert a) {
      return new AnomalyAlertResponse(
          a.getId(),
          a.getSubjectType(),
          a.getSubjectId(),
          a.getObservedMinor(),
          a.getScore(),
          a.getSeverity(),
          a.getStatus(),
          a.getDetail());
    }
  }

  public record AssistantResponse(
      String answer, double confidence, boolean modelAvailable, boolean injectionDetected) {

    static AssistantResponse from(AiAutomationService.AssistantResponse a) {
      return new AssistantResponse(
          a.answer(), a.confidence(), a.modelAvailable(), a.injectionDetected());
    }
  }

  /**
   * Parses the stored suggestion JSON string into a JsonNode so the response nests an object; on
   * any parse failure it degrades to the raw string (never fails the response).
   */
  private static JsonNode parseSuggestion(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      return TextNode.valueOf(json);
    }
  }
}

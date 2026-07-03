package com.paywithease.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.ai.domain.AiCategorizer;
import com.paywithease.ai.domain.AnomalyDetector;
import com.paywithease.ai.domain.CashflowPredictor;
import com.paywithease.ai.domain.ConfidencePolicy;
import com.paywithease.ai.domain.ParsedVoiceIntent;
import com.paywithease.ai.domain.PromptInjectionScanner;
import com.paywithease.ai.domain.SuggestionKind;
import com.paywithease.ai.domain.VoiceIntentParser;
import com.paywithease.ai.infrastructure.AiFeedback;
import com.paywithease.ai.infrastructure.AiFeedbackRepository;
import com.paywithease.ai.infrastructure.AiSuggestion;
import com.paywithease.ai.infrastructure.AiSuggestionRepository;
import com.paywithease.ai.infrastructure.AnomalyAlert;
import com.paywithease.ai.infrastructure.AnomalyAlertRepository;
import com.paywithease.ai.infrastructure.VoiceDraft;
import com.paywithease.ai.infrastructure.VoiceDraftRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Governance-first AI automation. Every AI output is scored, passed through {@link
 * ConfidencePolicy}, and stored with its decision; low-confidence and statutory outputs are never
 * auto-applied, humans accept/reject with feedback captured, uploaded text is scanned for prompt
 * injection, and the assistant degrades gracefully when no model is available. All reads/writes are
 * tenant-scoped, so no cross-tenant data can leak.
 */
@Service
public class AiAutomationService {

  private static final String SOURCE = "ai-automation-service";

  private final AiSuggestionRepository suggestions;
  private final AnomalyAlertRepository alerts;
  private final AiFeedbackRepository feedback;
  private final VoiceDraftRepository voiceDrafts;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final AiAssistantPort assistant;
  private final VoiceIntentParser voiceIntentParser;
  private final VoiceDraftMaterializer voiceDraftMaterializer;
  private final ConfidencePolicy policy;

  public AiAutomationService(
      AiSuggestionRepository suggestions,
      AnomalyAlertRepository alerts,
      AiFeedbackRepository feedback,
      VoiceDraftRepository voiceDrafts,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      AiAssistantPort assistant,
      VoiceIntentParser voiceIntentParser,
      VoiceDraftMaterializer voiceDraftMaterializer,
      @Value("${peb.ai.auto-apply-threshold:0.90}") double autoApplyThreshold,
      @Value("${peb.ai.review-threshold:0.50}") double reviewThreshold) {
    this.suggestions = suggestions;
    this.alerts = alerts;
    this.feedback = feedback;
    this.voiceDrafts = voiceDrafts;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.assistant = assistant;
    this.voiceIntentParser = voiceIntentParser;
    this.voiceDraftMaterializer = voiceDraftMaterializer;
    this.policy = new ConfidencePolicy(autoApplyThreshold, reviewThreshold);
  }

  // -------------------- Suggestions --------------------

  /** Classify a transaction narration into a category as a governed suggestion. */
  @Transactional
  public AiSuggestion classifyTransaction(String subjectId, String narration) {
    AiCategorizer.Category cat = AiCategorizer.classify(narration);
    ObjectNode value =
        objectMapper
            .createObjectNode()
            .put("category", cat.category())
            .put("rationale", cat.rationale());
    return createSuggestion(
        SuggestionKind.TRANSACTION_CATEGORY,
        "BANK_TRANSACTION",
        subjectId,
        value,
        cat.confidence(),
        "AiCategorizer/v1");
  }

  /**
   * Record an OCR bank-detail extraction as a suggestion. By governance this is never auto-applied
   * — the user must review before the detail is saved.
   */
  @Transactional
  public AiSuggestion reviewBankDetailExtraction(
      String subjectId, Map<String, String> extractedFields, double confidence) {
    ObjectNode value = objectMapper.valueToTree(extractedFields);
    return createSuggestion(
        SuggestionKind.BANK_DETAIL_EXTRACTION,
        "BANK_DETAIL",
        subjectId,
        value,
        confidence,
        "OCR/review-required");
  }

  /** Advisory next-period cashflow forecast, stored as a (never auto-applied) suggestion. */
  @Transactional
  public AiSuggestion forecastCashflow(List<Long> periodNets) {
    CashflowPredictor.Forecast f = CashflowPredictor.predict(periodNets);
    ObjectNode value =
        objectMapper
            .createObjectNode()
            .put("projectedNetMinor", f.projectedNetMinor())
            .put("basis", f.basis());
    return createSuggestion(
        SuggestionKind.CASHFLOW_FORECAST,
        "CASHFLOW",
        null,
        value,
        f.confidence(),
        "CashflowPredictor/v1");
  }

  private AiSuggestion createSuggestion(
      SuggestionKind kind,
      String subjectType,
      String subjectId,
      ObjectNode value,
      double confidence,
      String modelRef) {
    String tenantId = TenantContext.requireTenantId();
    if (subjectId != null
        && suggestions.existsByTenantIdAndSubjectTypeAndSubjectIdAndKind(
            tenantId, subjectType, subjectId, kind.name())) {
      // Idempotent: a suggestion of this kind already exists for this subject.
      return suggestions.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
          .filter(
              s ->
                  kind.name().equals(s.getKind())
                      && subjectType.equals(s.getSubjectType())
                      && subjectId.equals(s.getSubjectId()))
          .findFirst()
          .orElseThrow(() -> ApiException.notFound("Suggestion"));
    }
    ConfidencePolicy.Decision decision = policy.decide(kind, confidence);
    String status = decision == ConfidencePolicy.Decision.AUTO_APPLY ? "AUTO_APPLIED" : "PROPOSED";
    String suggestionJson = value.toString();
    AiSuggestion suggestion =
        new AiSuggestion(
            Ulid.newId(),
            tenantId,
            kind.name(),
            subjectType,
            subjectId,
            suggestionJson,
            BigDecimal.valueOf(confidence),
            decision.name(),
            status,
            modelRef,
            clock.instant());
    suggestions.save(suggestion);
    audit.record(
        "AI_SUGGESTION_CREATED",
        "ai_suggestion",
        suggestion.getId(),
        Map.of("kind", kind.name(), "decision", decision.name(), "confidence", confidence));
    emitSuggestion(suggestion);
    return suggestion;
  }

  @Transactional
  public AiSuggestion acceptSuggestion(String suggestionId) {
    AiSuggestion s = loadSuggestion(suggestionId);
    s.accept(actor(), clock.instant());
    suggestions.save(s);
    audit.record("AI_SUGGESTION_ACCEPTED", "ai_suggestion", suggestionId, Map.of());
    return s;
  }

  @Transactional
  public AiSuggestion rejectSuggestion(String suggestionId) {
    AiSuggestion s = loadSuggestion(suggestionId);
    s.reject(actor(), clock.instant());
    suggestions.save(s);
    audit.record("AI_SUGGESTION_REJECTED", "ai_suggestion", suggestionId, Map.of());
    return s;
  }

  @Transactional
  public AiFeedback submitFeedback(String suggestionId, boolean helpful, String note) {
    String tenantId = TenantContext.requireTenantId();
    loadSuggestion(suggestionId); // ensures the suggestion belongs to this tenant
    AiFeedback fb =
        new AiFeedback(
            Ulid.newId(), tenantId, suggestionId, helpful, note, actor(), clock.instant());
    feedback.save(fb);
    audit.record("AI_FEEDBACK_GIVEN", "ai_feedback", fb.getId(), Map.of("helpful", helpful));
    return fb;
  }

  @Transactional(readOnly = true)
  public AiSuggestion getSuggestion(String id) {
    return loadSuggestion(id);
  }

  @Transactional(readOnly = true)
  public List<AiSuggestion> listSuggestions(String status) {
    String tenantId = TenantContext.requireTenantId();
    return status == null || status.isBlank()
        ? suggestions.findByTenantIdOrderByCreatedAtDesc(tenantId)
        : suggestions.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
  }

  // -------------------- Anomalies --------------------

  @Transactional
  public AnomalyDetector.Result detectAnomaly(
      String subjectType, String subjectId, String metric, List<Long> history, long observed) {
    AnomalyDetector.Result result = AnomalyDetector.evaluate(history, observed);
    if (result.anomaly()) {
      String tenantId = TenantContext.requireTenantId();
      AnomalyAlert alert =
          new AnomalyAlert(
              Ulid.newId(),
              tenantId,
              subjectType,
              subjectId,
              metric,
              observed,
              BigDecimal.valueOf(result.score()),
              result.severity().name(),
              result.detail(),
              clock.instant());
      alerts.save(alert);
      audit.record(
          "ANOMALY_DETECTED",
          "anomaly_alert",
          alert.getId(),
          Map.of("severity", result.severity().name(), "score", result.score()));
      emitAnomaly(alert);
    }
    return result;
  }

  @Transactional
  public AnomalyAlert acknowledgeAnomaly(String alertId) {
    AnomalyAlert alert = loadAlert(alertId);
    alert.acknowledge(actor());
    alerts.save(alert);
    audit.record("ANOMALY_ACKNOWLEDGED", "anomaly_alert", alertId, Map.of());
    return alert;
  }

  @Transactional
  public AnomalyAlert dismissAnomaly(String alertId) {
    AnomalyAlert alert = loadAlert(alertId);
    alert.dismiss(actor());
    alerts.save(alert);
    audit.record("ANOMALY_DISMISSED", "anomaly_alert", alertId, Map.of());
    return alert;
  }

  @Transactional(readOnly = true)
  public List<AnomalyAlert> listAlerts(String status) {
    String tenantId = TenantContext.requireTenantId();
    return status == null || status.isBlank()
        ? alerts.findByTenantIdOrderByCreatedAtDesc(tenantId)
        : alerts.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
  }

  // -------------------- NL assistant --------------------

  public record AssistantResponse(
      String answer, double confidence, boolean modelAvailable, boolean injectionDetected) {}

  /**
   * Answer a finance question. The question is scanned for prompt injection and neutralised before
   * it goes near the model; the model only receives tenant-scoped context; and the assistant is
   * advisory — it cannot perform any statutory or financial action. Degrades to a manual-review
   * response when no model is available.
   */
  @Transactional(readOnly = true)
  public AssistantResponse askAssistant(String question) {
    String tenantId = TenantContext.requireTenantId();
    PromptInjectionScanner.ScanResult scan = PromptInjectionScanner.scan(question);
    if (scan.suspicious()) {
      audit.record(
          "AI_PROMPT_INJECTION_BLOCKED",
          "ai_assistant",
          tenantId,
          Map.of("matches", scan.matches().size()));
    }
    // Context is assembled only from this tenant's own suggestions/alerts — never another tenant's.
    String tenantContext =
        "openSuggestions="
            + suggestions.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PROPOSED").size()
            + "; openAlerts="
            + alerts.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "OPEN").size();
    if (!assistant.isAvailable()) {
      AiAssistantPort.Answer a = assistant.answer(tenantId, scan.sanitizedText(), tenantContext);
      return new AssistantResponse(a.text(), a.confidence(), false, scan.suspicious());
    }
    AiAssistantPort.Answer a = assistant.answer(tenantId, scan.sanitizedText(), tenantContext);
    return new AssistantResponse(a.text(), a.confidence(), a.modelAvailable(), scan.suspicious());
  }

  // -------------------- Voice drafts --------------------

  @Transactional
  public VoiceDraft parseVoice(String transcript) {
    String tenantId = TenantContext.requireTenantId();
    PromptInjectionScanner.ScanResult scan = PromptInjectionScanner.scan(transcript);
    ParsedVoiceIntent parsed = voiceIntentParser.parse(scan.sanitizedText());
    VoiceDraft draft =
        new VoiceDraft(
            Ulid.newId(),
            tenantId,
            transcript,
            scan.sanitizedText(),
            parsed,
            toJson(parsed.fields()),
            toJson(parsed.missingFields()),
            scan.suspicious(),
            actor(),
            clock.instant());
    voiceDrafts.save(draft);
    audit.record(
        "VOICE_DRAFT_CREATED",
        "voice_draft",
        draft.getId(),
        Map.of(
            "intent", draft.getIntent(),
            "confidence", draft.getConfidence(),
            "suspicious", scan.suspicious()));
    emitVoice("VOICE_DRAFT_CREATED", draft);
    return draft;
  }

  @Transactional(readOnly = true)
  public List<VoiceDraft> listVoiceDrafts(String status) {
    String tenantId = TenantContext.requireTenantId();
    return status == null || status.isBlank()
        ? voiceDrafts.findByTenantIdOrderByCreatedAtDesc(tenantId)
        : voiceDrafts.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
  }

  @Transactional(readOnly = true)
  public VoiceDraft getVoiceDraft(String id) {
    return loadVoiceDraft(id);
  }

  @Transactional
  public VoiceDraft approveVoiceDraft(String id, Map<String, Object> reviewedFields) {
    VoiceDraft draft = loadVoiceDraft(id);
    if ("APPROVED".equals(draft.getStatus())) {
      return draft;
    }
    if (!"NEEDS_REVIEW".equals(draft.getStatus())) {
      throw new ApiException(ErrorCode.CONFLICT, "Only pending voice drafts can be approved");
    }
    Map<String, Object> fields =
        reviewedFields == null || reviewedFields.isEmpty()
            ? readMap(draft.getFieldsJson())
            : reviewedFields;
    validateVoiceDraftApproval(draft, fields);
    if (draft.getMaterializedRef() == null) {
      VoiceDraftMaterializer.MaterializedRecord materialized =
          voiceDraftMaterializer.materialize(draft, fields);
      draft.approve(materialized.id(), actor(), clock.instant());
    }
    voiceDrafts.save(draft);
    audit.record(
        "VOICE_DRAFT_APPROVED",
        "voice_draft",
        draft.getId(),
        Map.of("intent", draft.getIntent(), "materializedRef", draft.getMaterializedRef()));
    emitVoice("VOICE_DRAFT_APPROVED", draft);
    return draft;
  }

  @Transactional
  public VoiceDraft rejectVoiceDraft(String id, String reason) {
    VoiceDraft draft = loadVoiceDraft(id);
    if ("REJECTED".equals(draft.getStatus())) {
      return draft;
    }
    if (!"NEEDS_REVIEW".equals(draft.getStatus())) {
      throw new ApiException(ErrorCode.CONFLICT, "Only pending voice drafts can be rejected");
    }
    draft.reject(reason, actor(), clock.instant());
    voiceDrafts.save(draft);
    audit.record("VOICE_DRAFT_REJECTED", "voice_draft", draft.getId(), Map.of());
    emitVoice("VOICE_DRAFT_REJECTED", draft);
    return draft;
  }

  // -------------------- helpers --------------------

  private AiSuggestion loadSuggestion(String id) {
    return suggestions
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Suggestion"));
  }

  private AnomalyAlert loadAlert(String id) {
    return alerts
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Anomaly alert"));
  }

  private VoiceDraft loadVoiceDraft(String id) {
    return voiceDrafts
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Voice draft"));
  }

  private String actor() {
    return TenantContext.actorId()
        .orElseThrow(
            () -> new ApiException(ErrorCode.UNAUTHENTICATED, "No acting user in context"));
  }

  private void emitSuggestion(AiSuggestion s) {
    ObjectNode payload =
        objectMapper
            .createObjectNode()
            .put("suggestionId", s.getId())
            .put("kind", s.getKind())
            .put("decision", s.getDecision())
            .put("status", s.getStatus());
    payload.put("confidence", s.getConfidence());
    emit("AI_SUGGESTION_CREATED", s.getTenantId(), s.getId(), payload);
  }

  private void emitAnomaly(AnomalyAlert a) {
    ObjectNode payload =
        objectMapper
            .createObjectNode()
            .put("alertId", a.getId())
            .put("subjectType", a.getSubjectType())
            .put("subjectId", a.getSubjectId())
            .put("severity", a.getSeverity());
    payload.put("observedMinor", a.getObservedMinor());
    emit("ANOMALY_DETECTED", a.getTenantId(), a.getId(), payload);
  }

  private void emitVoice(String eventType, VoiceDraft draft) {
    ObjectNode payload =
        objectMapper
            .createObjectNode()
            .put("voiceDraftId", draft.getId())
            .put("intent", draft.getIntent())
            .put("status", draft.getStatus())
            .put("suspicious", draft.isSuspicious());
    emit(eventType, draft.getTenantId(), draft.getId(), payload);
  }

  private void emit(String eventType, String tenantId, String aggregateId, ObjectNode payload) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static void validateVoiceDraftApproval(VoiceDraft draft, Map<String, Object> fields) {
    if ("CREATE_COMMITMENT".equals(draft.getIntent())) {
      for (String required : List.of("counterpartyName", "amountMinor", "dueDate")) {
        Object value = fields.get(required);
        if (value == null || value.toString().isBlank()) {
          throw new ApiException(
              ErrorCode.VALIDATION_FAILED, "Voice draft is missing required field: " + required);
        }
      }
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not serialize voice draft");
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readMap(String json) {
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (Exception e) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not read voice draft fields");
    }
  }
}

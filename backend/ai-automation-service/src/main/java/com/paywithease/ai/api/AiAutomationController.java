package com.paywithease.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.ai.application.AiAutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Governance-first AI automation API — transaction classification, OCR bank-detail review, cashflow
 * forecasting, anomaly detection, and an advisory NL assistant.
 *
 * <p>Every AI output response carries its confidence/score so the UI can always surface it. AI is
 * advisory: low-confidence and statutory outputs are never auto-applied — a human accepts/rejects
 * suggestions and acknowledges/dismisses anomaly alerts, and those governance decisions are guarded
 * by a higher-privilege authority than merely producing a suggestion. There is deliberately no
 * autonomous filing action here. All reads/writes are tenant-scoped in the service via {@code
 * TenantContext}; no endpoint accepts a tenantId, so no cross-tenant access is possible. The acting
 * user is taken from the JWT subject inside the service (via {@code TenantContext.actorId()}), not
 * from any request field.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(
    name = "ai",
    description =
        "Governance-first AI: classification, OCR bank-detail review, anomaly detection,"
            + " cashflow forecast, and an advisory NL assistant (every output shows confidence)")
public class AiAutomationController {

  private final AiAutomationService service;
  private final ObjectMapper objectMapper;

  public AiAutomationController(AiAutomationService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  // -------------------- Suggestions --------------------

  @PostMapping("/suggestions/classify-transaction")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(summary = "Classify a transaction narration into a category as a governed suggestion")
  public AiDtos.SuggestionResponse classifyTransaction(
      @Valid @RequestBody AiDtos.ClassifyTransactionRequest body) {
    return AiDtos.SuggestionResponse.from(
        service.classifyTransaction(body.subjectId(), body.narration()), objectMapper);
  }

  @PostMapping("/suggestions/bank-detail")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(summary = "Record an OCR bank-detail extraction as a suggestion (never auto-applied)")
  public AiDtos.SuggestionResponse bankDetail(@Valid @RequestBody AiDtos.BankDetailRequest body) {
    return AiDtos.SuggestionResponse.from(
        service.reviewBankDetailExtraction(body.subjectId(), body.fields(), body.confidence()),
        objectMapper);
  }

  @PostMapping("/suggestions/cashflow-forecast")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(summary = "Advisory next-period cashflow forecast, stored as a suggestion")
  public AiDtos.SuggestionResponse cashflowForecast(
      @Valid @RequestBody AiDtos.CashflowForecastRequest body) {
    return AiDtos.SuggestionResponse.from(
        service.forecastCashflow(body.periodNets()), objectMapper);
  }

  @GetMapping("/suggestions")
  @Operation(summary = "List suggestions for the tenant (optionally filtered by status)")
  public List<AiDtos.SuggestionResponse> listSuggestions(
      @RequestParam(required = false) String status) {
    return service.listSuggestions(status).stream()
        .map(s -> AiDtos.SuggestionResponse.from(s, objectMapper))
        .toList();
  }

  @GetMapping("/suggestions/{id}")
  @Operation(summary = "Get a suggestion")
  public AiDtos.SuggestionResponse getSuggestion(@PathVariable String id) {
    return AiDtos.SuggestionResponse.from(service.getSuggestion(id), objectMapper);
  }

  @PostMapping("/suggestions/{id}/accept")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Human approval of an AI suggestion (governance decision)")
  public AiDtos.SuggestionResponse acceptSuggestion(@PathVariable String id) {
    return AiDtos.SuggestionResponse.from(service.acceptSuggestion(id), objectMapper);
  }

  @PostMapping("/suggestions/{id}/reject")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Human rejection of an AI suggestion (governance decision)")
  public AiDtos.SuggestionResponse rejectSuggestion(@PathVariable String id) {
    return AiDtos.SuggestionResponse.from(service.rejectSuggestion(id), objectMapper);
  }

  @PostMapping("/suggestions/{id}/feedback")
  @Operation(summary = "Give feedback on a suggestion (keeps the review loop auditable)")
  public AiDtos.FeedbackResponse feedback(
      @PathVariable String id, @Valid @RequestBody AiDtos.FeedbackRequest body) {
    return AiDtos.FeedbackResponse.from(service.submitFeedback(id, body.helpful(), body.note()));
  }

  // -------------------- Anomalies --------------------

  @PostMapping("/anomalies/detect")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(
      summary =
          "Evaluate an observed metric against its history; raises an alert only if anomalous")
  public AiDtos.AnomalyResultResponse detectAnomaly(
      @Valid @RequestBody AiDtos.DetectAnomalyRequest body) {
    return AiDtos.AnomalyResultResponse.from(
        service.detectAnomaly(
            body.subjectType(), body.subjectId(), body.metric(), body.history(), body.observed()));
  }

  @GetMapping("/anomalies")
  @Operation(summary = "List anomaly alerts for the tenant (optionally filtered by status)")
  public List<AiDtos.AnomalyAlertResponse> listAlerts(
      @RequestParam(required = false) String status) {
    return service.listAlerts(status).stream().map(AiDtos.AnomalyAlertResponse::from).toList();
  }

  @PostMapping("/anomalies/{id}/acknowledge")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Acknowledge an anomaly alert (governance decision)")
  public AiDtos.AnomalyAlertResponse acknowledgeAnomaly(@PathVariable String id) {
    return AiDtos.AnomalyAlertResponse.from(service.acknowledgeAnomaly(id));
  }

  @PostMapping("/anomalies/{id}/dismiss")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Dismiss an anomaly alert (governance decision)")
  public AiDtos.AnomalyAlertResponse dismissAnomaly(@PathVariable String id) {
    return AiDtos.AnomalyAlertResponse.from(service.dismissAnomaly(id));
  }

  // -------------------- Assistant --------------------

  @PostMapping("/assistant/ask")
  @Operation(
      summary =
          "Ask the advisory NL assistant a finance question (degrades to manual when no model)")
  public AiDtos.AssistantResponse ask(@Valid @RequestBody AiDtos.AskAssistantRequest body) {
    return AiDtos.AssistantResponse.from(service.askAssistant(body.question()));
  }

  // -------------------- Voice drafts --------------------

  @PostMapping("/voice/parse")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(summary = "Parse a voice transcript into a reviewed financial draft")
  public AiDtos.VoiceDraftResponse parseVoice(@Valid @RequestBody AiDtos.ParseVoiceRequest body) {
    return AiDtos.VoiceDraftResponse.from(service.parseVoice(body.transcript()), objectMapper);
  }

  @GetMapping("/voice/drafts")
  @Operation(summary = "List voice drafts for review")
  public List<AiDtos.VoiceDraftResponse> listVoiceDrafts(
      @RequestParam(required = false) String status) {
    return service.listVoiceDrafts(status).stream()
        .map(d -> AiDtos.VoiceDraftResponse.from(d, objectMapper))
        .toList();
  }

  @GetMapping("/voice/drafts/{id}")
  @Operation(summary = "Get a voice draft")
  public AiDtos.VoiceDraftResponse getVoiceDraft(@PathVariable String id) {
    return AiDtos.VoiceDraftResponse.from(service.getVoiceDraft(id), objectMapper);
  }

  @PostMapping("/voice/drafts/{id}/approve")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Approve a voice draft; only then may a domain record be created")
  public AiDtos.VoiceDraftResponse approveVoiceDraft(
      @PathVariable String id,
      @RequestBody(required = false) AiDtos.ApproveVoiceDraftRequest body) {
    return AiDtos.VoiceDraftResponse.from(
        service.approveVoiceDraft(id, body == null ? null : body.fields()), objectMapper);
  }

  @PostMapping("/voice/drafts/{id}/reject")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA','CASHIER')")
  @Operation(summary = "Reject/discard a voice draft")
  public AiDtos.VoiceDraftResponse rejectVoiceDraft(
      @PathVariable String id, @RequestBody(required = false) AiDtos.RejectVoiceDraftRequest body) {
    return AiDtos.VoiceDraftResponse.from(
        service.rejectVoiceDraft(id, body == null ? null : body.reason()), objectMapper);
  }
}

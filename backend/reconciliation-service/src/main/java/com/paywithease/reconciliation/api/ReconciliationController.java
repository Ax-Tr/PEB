package com.paywithease.reconciliation.api;

import com.paywithease.reconciliation.application.ReconciliationService;
import com.paywithease.reconciliation.domain.ReconException;
import com.paywithease.reconciliation.domain.ReconMatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation API — run matching, review suggestions/exceptions, confirm/reject/manual match.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(
    name = "reconciliation",
    description = "Weighted matching of imported bank rows against internal records")
public class ReconciliationController {

  private final ReconciliationService service;

  public ReconciliationController(ReconciliationService service) {
    this.service = service;
  }

  @PostMapping("/run")
  @Operation(summary = "Run the match engine over all unmatched external items")
  public ReconciliationDtos.RunResponse run() {
    ReconciliationService.RunResult r = service.run();
    return new ReconciliationDtos.RunResponse(
        r.autoMatched(), r.suggested(), r.exceptionsCreated());
  }

  @GetMapping("/suggestions")
  @Operation(summary = "List suggested matches awaiting confirm/reject")
  public List<ReconciliationDtos.MatchResponse> suggestions() {
    return service.suggestions().stream().map(this::toMatch).toList();
  }

  @GetMapping("/exceptions")
  @Operation(summary = "List open exceptions (items with no confident match)")
  public List<ReconciliationDtos.ExceptionResponse> exceptions() {
    return service.openExceptions().stream().map(this::toException).toList();
  }

  @PostMapping("/matches/{id}/confirm")
  @Operation(summary = "Confirm a suggested match")
  public ReconciliationDtos.MatchResponse confirm(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return toMatch(service.confirmMatch(id, jwt.getSubject()));
  }

  @PostMapping("/matches/{id}/reject")
  @Operation(summary = "Reject a suggested match")
  public ReconciliationDtos.MatchResponse reject(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return toMatch(service.rejectMatch(id, jwt.getSubject()));
  }

  @PostMapping("/matches/manual")
  @Operation(summary = "Manually pair an external item with an internal item")
  public ReconciliationDtos.MatchResponse manual(
      @Valid @RequestBody ReconciliationDtos.ManualMatchRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    return toMatch(
        service.manualMatch(body.externalItemId(), body.internalItemId(), jwt.getSubject()));
  }

  private ReconciliationDtos.MatchResponse toMatch(ReconMatch m) {
    return new ReconciliationDtos.MatchResponse(
        m.getId(), m.getExternalItemId(), m.getInternalItemId(), m.getStatus());
  }

  private ReconciliationDtos.ExceptionResponse toException(ReconException e) {
    return new ReconciliationDtos.ExceptionResponse(
        e.getId(), e.getItemId(), e.getReason(), e.getStatus());
  }
}

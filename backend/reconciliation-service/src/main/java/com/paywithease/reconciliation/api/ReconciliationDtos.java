package com.paywithease.reconciliation.api;

import jakarta.validation.constraints.NotBlank;

/** Request/response DTOs for the /reconciliation API. */
public final class ReconciliationDtos {

  private ReconciliationDtos() {}

  public record MatchResponse(
      String id, String externalItemId, String internalItemId, String status) {}

  public record ExceptionResponse(String id, String itemId, String reason, String status) {}

  public record RunResponse(int autoMatched, int suggested, int exceptionsCreated) {}

  public record ManualMatchRequest(
      @NotBlank String externalItemId, @NotBlank String internalItemId) {}
}

package com.paywithease.common.error;

import org.springframework.http.HttpStatus;

/** Stable, documented error codes returned in the RFC-7807 {@code problem+json} body. */
public enum ErrorCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "One or more fields are invalid"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Not permitted for this role"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
  IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "Idempotency key reused with a different request"),
  CONFLICT(HttpStatus.CONFLICT, "Conflicting state"),
  TENANT_CONTEXT_MISSING(HttpStatus.BAD_REQUEST, "Tenant could not be resolved"),
  STEP_UP_REQUIRED(HttpStatus.UNAUTHORIZED, "Step-up authentication required for this action"),
  MONTH_LOCKED(HttpStatus.CONFLICT, "Financial period is locked"),
  UNBALANCED_JOURNAL(HttpStatus.UNPROCESSABLE_ENTITY, "Debits and credits do not balance"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

  private final HttpStatus status;
  private final String defaultDetail;

  ErrorCode(HttpStatus status, String defaultDetail) {
    this.status = status;
    this.defaultDetail = defaultDetail;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultDetail() {
    return defaultDetail;
  }
}

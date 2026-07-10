package com.paywithease.common.error;

import com.paywithease.common.tenant.TenantContext;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps exceptions to RFC-7807 {@code application/problem+json}. Never leaks stack traces or
 * sensitive data; every problem carries the stable error code and the request correlation id.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TYPE_BASE = "https://errors.paywithease.com/";

  @ExceptionHandler(ApiException.class)
  public ProblemDetail handleApi(ApiException ex) {
    return problem(ex.errorCode(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse(ErrorCode.VALIDATION_FAILED.defaultDetail());
    return problem(ErrorCode.VALIDATION_FAILED, detail);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraint(ConstraintViolationException ex) {
    return problem(ErrorCode.VALIDATION_FAILED, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    // TenantContext.requireTenantId throws IllegalStateException when tenant is missing.
    return problem(ErrorCode.TENANT_CONTEXT_MISSING, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    // Intentionally opaque detail; real cause is logged, not returned.
    log.error("Unexpected error occurred", ex);
    return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultDetail());
  }

  private ProblemDetail problem(ErrorCode code, String detail) {
    HttpStatus status = code.status();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(TYPE_BASE + code.name()));
    pd.setTitle(code.name());
    pd.setProperty("code", code.name());
    TenantContext.current().ifPresent(p -> pd.setProperty("correlationId", p.correlationId()));
    return pd;
  }
}

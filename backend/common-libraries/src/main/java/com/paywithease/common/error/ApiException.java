package com.paywithease.common.error;

/** Base application exception carrying a stable {@link ErrorCode}. */
public class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient ErrorCode errorCode;

  public ApiException(ErrorCode errorCode, String message) {
    super(message == null ? errorCode.defaultDetail() : message);
    this.errorCode = errorCode;
  }

  public ApiException(ErrorCode errorCode) {
    this(errorCode, errorCode.defaultDetail());
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public static ApiException notFound(String what) {
    return new ApiException(ErrorCode.NOT_FOUND, what + " not found");
  }

  public static ApiException forbidden() {
    return new ApiException(ErrorCode.FORBIDDEN);
  }
}

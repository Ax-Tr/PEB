package com.paywithease.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request/response DTOs for the auth API. Validation is declared on the record components. */
public final class AuthDtos {

  private AuthDtos() {}

  public record OtpRequest(
      @NotBlank @Pattern(regexp = "(\\+?91)?[6-9]\\d{9}", message = "invalid Indian mobile number")
          String mobile) {}

  public record DeviceDto(String fingerprint, String platform, String model) {}

  public record OtpVerify(
      @NotBlank @Pattern(regexp = "(\\+?91)?[6-9]\\d{9}") String mobile,
      @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp,
      @Valid DeviceDto device,
      String consentNoticeVersion) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record LogoutRequest(@NotBlank @Size(max = 26) String sessionId) {}

  public record AuthResponse(
      String userId,
      String tenantId,
      List<String> roles,
      String tokenType,
      String accessToken,
      long expiresIn,
      String refreshToken,
      String sessionId,
      boolean newUser) {}

  public record SessionDto(
      String id, String deviceId, String status, Instant issuedAt, Instant expiresAt) {}

  public record LinkTenantRequest(@NotBlank @Size(max = 26) String tenantId) {}
}

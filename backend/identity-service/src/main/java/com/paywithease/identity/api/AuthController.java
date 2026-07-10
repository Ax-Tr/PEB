package com.paywithease.identity.api;

import com.paywithease.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** OTP auth, token refresh, and session management. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "OTP login, token refresh, sessions")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/otp/request")
  @Operation(summary = "Request an OTP for a mobile number")
  public Map<String, Object> requestOtp(@Valid @RequestBody AuthDtos.OtpRequest body) {
    var result = authService.requestOtp(body.mobile());
    var response = new java.util.HashMap<String, Object>();
    response.put("status", "sent");
    response.put("expiresIn", result.ttl());
    if (authService.isLogOtpForDev()) {
      response.put("otp", result.otp());
    }
    return response;
  }

  @PostMapping("/otp/verify")
  @Operation(summary = "Verify OTP and issue tokens (self-onboards a new user)")
  public AuthDtos.AuthResponse verifyOtp(
      @Valid @RequestBody AuthDtos.OtpVerify body, HttpServletRequest request) {
    var device =
        body.device() == null
            ? null
            : new AuthService.DeviceInfo(
                body.device().fingerprint(), body.device().platform(), body.device().model());
    var result =
        authService.verifyOtpAndLogin(
            body.mobile(),
            body.otp(),
            device,
            clientIp(request),
            request.getHeader("User-Agent"),
            body.consentNoticeVersion());
    return toResponse(result);
  }

  @PostMapping("/token/refresh")
  @Operation(summary = "Rotate the refresh token and issue a new access token")
  public AuthDtos.AuthResponse refresh(
      @Valid @RequestBody AuthDtos.RefreshRequest body, HttpServletRequest request) {
    return toResponse(
        authService.refresh(
            body.refreshToken(), clientIp(request), request.getHeader("User-Agent")));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke a session")
  public void logout(
      @Valid @RequestBody AuthDtos.LogoutRequest body, @AuthenticationPrincipal Jwt jwt) {
    authService.logout(body.sessionId(), jwt.getSubject());
  }

  @GetMapping("/sessions")
  @Operation(summary = "List active sessions for the current user")
  public List<AuthDtos.SessionDto> sessions(@AuthenticationPrincipal Jwt jwt) {
    return authService.sessions(jwt.getSubject()).stream()
        .map(
            s ->
                new AuthDtos.SessionDto(
                    s.getId(),
                    s.getDeviceId(),
                    s.getStatus().name(),
                    s.getIssuedAt(),
                    s.getExpiresAt()))
        .toList();
  }

  @DeleteMapping("/sessions/{id}")
  public ResponseEntity<Void> revoke(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    authService.logout(id, jwt.getSubject());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/link-tenant")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Link a newly-created tenant/business to the calling user's identity")
  public void linkTenant(
      @Valid @RequestBody AuthDtos.LinkTenantRequest body, @AuthenticationPrincipal Jwt jwt) {
    authService.linkTenant(jwt.getSubject(), body.tenantId());
  }

  private static AuthDtos.AuthResponse toResponse(AuthService.AuthResult r) {
    return new AuthDtos.AuthResponse(
        r.userId(),
        r.tenantId(),
        r.roles(),
        "Bearer",
        r.accessToken(),
        r.accessExpiresIn(),
        r.refreshToken(),
        r.sessionId(),
        r.newUser());
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    return (forwarded != null && !forwarded.isBlank())
        ? forwarded.split(",")[0].trim()
        : request.getRemoteAddr();
  }
}

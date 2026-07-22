package com.paywithease.ai.application;

import com.paywithease.ai.domain.VoiceIntent;
import com.paywithease.ai.infrastructure.VoiceDraft;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.tenant.TenantContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpVoiceDraftMaterializer implements VoiceDraftMaterializer {

  private final RestClient restClient;

  public HttpVoiceDraftMaterializer(
      RestClient.Builder builder,
      @Value("${peb.ai.commitment-uri:http://localhost:8102}") String commitmentUri) {
    this.restClient = builder.baseUrl(commitmentUri).build();
  }

  @Override
  public MaterializedRecord materialize(VoiceDraft draft, Map<String, Object> reviewedFields) {
    if (!VoiceIntent.CREATE_COMMITMENT.name().equals(draft.getIntent())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "This voice intent is not materializable yet");
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("counterpartyType", value(reviewedFields, "counterpartyType", "CUSTOMER"));
    body.put("counterpartyName", value(reviewedFields, "counterpartyName", null));
    body.put("sourceType", "VOICE");
    body.put("sourceRef", draft.getId());
    body.put("description", value(reviewedFields, "description", draft.getSanitizedTranscript()));
    body.put("amountMinor", value(reviewedFields, "amountMinor", null));
    body.put("dueDate", value(reviewedFields, "dueDate", null));

    CommitmentResponse response =
        restClient
            .post()
            .uri("/api/v1/commitments")
            .header("Idempotency-Key", "voice-draft-" + draft.getId())
            .headers(HttpVoiceDraftMaterializer::tenantHeaders)
            .body(body)
            .retrieve()
            .body(CommitmentResponse.class);
    if (response == null || response.id() == null) {
      return new MaterializedRecord("COMMITMENT", Ulid.newId());
    }
    return new MaterializedRecord("COMMITMENT", response.id());
  }

  private static Object value(Map<String, Object> fields, String key, Object fallback) {
    Object value = fields.get(key);
    return value == null ? fallback : value;
  }

  private static void tenantHeaders(HttpHeaders headers) {
    TenantContext.current()
        .ifPresent(
            p -> {
              headers.set("X-Tenant-Id", p.tenantId());
              headers.set("X-Business-Id", p.businessId());
              headers.set("X-Actor-Id", p.actorId());
              headers.set("X-Correlation-Id", p.correlationId());
            });
    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
      headers.set(
          org.springframework.http.HttpHeaders.AUTHORIZATION,
          "Bearer " + jwtAuth.getToken().getTokenValue());
    } else if (auth != null
        && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
      headers.set(
          org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
    }
  }

  private record CommitmentResponse(String id) {}
}

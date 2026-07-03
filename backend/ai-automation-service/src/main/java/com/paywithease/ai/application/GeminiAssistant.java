package com.paywithease.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnExpression("'${peb.ai.gemini.api-key:}' != ''")
public class GeminiAssistant implements AiAssistantPort {

  private static final String SYSTEM_INSTRUCTION =
      """
      You are PayWithEase's advisory finance assistant for Indian MSMEs.
      Answer only from the tenant-scoped context and the user's sanitized question.
      Do not claim to file GST/TDS, post ledgers, initiate payouts, change bank details, or make any
      other state-changing action. When data is insufficient, say what the user should review in the
      app. Keep answers concise and practical.
      """;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;

  public GeminiAssistant(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${peb.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
          String baseUrl,
      @Value("${peb.ai.gemini.api-key}") String apiKey,
      @Value("${peb.ai.gemini.model:gemini-3-flash-preview}") String model) {
    this.restClient =
        builder
            .baseUrl(baseUrl)
            .defaultHeader("x-goog-api-key", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    this.objectMapper = objectMapper;
    this.model = model;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public Answer answer(String tenantId, String sanitizedQuestion, String tenantScopedContext) {
    try {
      JsonNode response =
          restClient
              .post()
              .uri("/models/{model}:generateContent", model)
              .body(requestBody(tenantId, sanitizedQuestion, tenantScopedContext))
              .retrieve()
              .body(JsonNode.class);
      String text = extractText(response);
      if (text.isBlank()) {
        return unavailableAnswer();
      }
      return new Answer(text, confidence(response), true);
    } catch (RestClientException | IllegalArgumentException e) {
      return unavailableAnswer();
    }
  }

  private Map<String, Object> requestBody(
      String tenantId, String sanitizedQuestion, String tenantScopedContext) {
    String prompt =
        "Tenant: "
            + tenantId
            + "\nTenant-scoped context: "
            + tenantScopedContext
            + "\nQuestion: "
            + sanitizedQuestion;
    Map<String, Object> textPart = Map.of("text", prompt);
    Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(textPart));
    Map<String, Object> systemInstruction =
        Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION)));

    Map<String, Object> generationConfig = new LinkedHashMap<>();
    generationConfig.put("temperature", 0.2);
    generationConfig.put("maxOutputTokens", 512);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("systemInstruction", systemInstruction);
    body.put("contents", List.of(userContent));
    body.put("generationConfig", generationConfig);
    return body;
  }

  private String extractText(JsonNode response) {
    if (response == null) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    JsonNode parts = response.at("/candidates/0/content/parts");
    if (parts.isArray()) {
      for (JsonNode part : parts) {
        JsonNode value = part.get("text");
        if (value != null && value.isTextual()) {
          text.append(value.asText());
        }
      }
    }
    return text.toString().trim();
  }

  private double confidence(JsonNode response) {
    JsonNode finishReason = response.at("/candidates/0/finishReason");
    if (finishReason.isTextual() && !"STOP".equals(finishReason.asText())) {
      return 0.55;
    }
    return 0.75;
  }

  private Answer unavailableAnswer() {
    return new Answer(
        "The AI assistant could not produce a reliable answer right now. Please review the relevant"
            + " report or dashboard manually.",
        0.0,
        false);
  }
}

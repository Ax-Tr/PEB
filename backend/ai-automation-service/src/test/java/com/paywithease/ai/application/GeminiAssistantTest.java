package com.paywithease.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiAssistantTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void callsGeminiWithTenantScopedContextAndExtractsText() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    GeminiAssistant assistant =
        new GeminiAssistant(
            builder, objectMapper, "https://gemini.test/v1beta", "test-key", "gemini-test");
    server
        .expect(once(), requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
        .andExpect(header("x-goog-api-key", "test-key"))
        .andExpect(jsonPath("$.systemInstruction.parts[0].text").exists())
        .andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(org.hamcrest.Matchers.containsString("tenant1")))
        .andExpect(
            jsonPath("$.contents[0].parts[0].text")
                .value(org.hamcrest.Matchers.containsString("openAlerts=1")))
        .andRespond(
            withSuccess(
                """
                {
                  "candidates":[{
                    "finishReason":"STOP",
                    "content":{"parts":[{"text":"Cash position is stable. Review open alerts."}]}
                  }]
                }
                """,
                MediaType.APPLICATION_JSON));

    AiAssistantPort.Answer answer =
        assistant.answer("tenant1", "How is cash?", "openSuggestions=2; openAlerts=1");

    assertThat(answer.modelAvailable()).isTrue();
    assertThat(answer.text()).isEqualTo("Cash position is stable. Review open alerts.");
    assertThat(answer.confidence()).isEqualTo(0.75);
    server.verify();
  }

  @Test
  void providerFailureFallsBackToManualReviewAnswer() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    GeminiAssistant assistant =
        new GeminiAssistant(
            builder, objectMapper, "https://gemini.test/v1beta", "test-key", "gemini-test");
    server
        .expect(once(), requestTo("https://gemini.test/v1beta/models/gemini-test:generateContent"))
        .andRespond(withServerError());

    AiAssistantPort.Answer answer =
        assistant.answer("tenant1", "How is cash?", "openSuggestions=2; openAlerts=1");

    assertThat(answer.modelAvailable()).isFalse();
    assertThat(answer.confidence()).isZero();
    assertThat(answer.text()).contains("could not produce a reliable answer");
    server.verify();
  }
}

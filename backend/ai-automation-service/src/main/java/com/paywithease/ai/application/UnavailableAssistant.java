package com.paywithease.ai.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default assistant adapter used when no real model is configured. It reports itself unavailable
 * and returns a manual-fallback answer, so the NL-assistant feature degrades gracefully to "review
 * manually" rather than failing — satisfying the "model unavailable" edge case. A production LLM
 * adapter can be provided as a bean to replace this one.
 */
@Configuration
public class UnavailableAssistant {

  @Bean
  @ConditionalOnMissingBean(AiAssistantPort.class)
  public AiAssistantPort fallbackAssistant() {
    return new AiAssistantPort() {
      @Override
      public boolean isAvailable() {
        return false;
      }

      @Override
      public Answer answer(String tenantId, String sanitizedQuestion, String tenantScopedContext) {
        return new Answer(
            "The AI assistant is not enabled in this environment. Please review the figures in the"
                + " relevant report or dashboard manually.",
            0.0,
            false);
      }
    };
  }
}

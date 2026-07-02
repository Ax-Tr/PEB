package com.paywithease.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI Automation Service — governance-first AI for MSME finance: transaction classification, OCR
 * bank-detail review, anomaly detection, cashflow forecasting, and an advisory NL assistant. Every
 * AI output is scored and surfaced with its confidence; low-confidence and statutory outputs are
 * never auto-applied (humans accept/reject); inputs are scanned for prompt injection; and the
 * assistant degrades gracefully to manual review when no model is available. All data is
 * tenant-scoped and auditable. It emits {@code AI_SUGGESTION_CREATED} / {@code ANOMALY_DETECTED}
 * via the transactional outbox.
 *
 * <p>The {@link java.time.Clock} and {@code ObjectMapper} beans come from common-libraries via the
 * {@code com.paywithease} component scan, and the fallback {@code AiAssistantPort} is provided by
 * {@code UnavailableAssistant} — none are redefined here.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class AiAutomationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiAutomationServiceApplication.class, args);
  }
}

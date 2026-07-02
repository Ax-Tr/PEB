package com.paywithease.ai.application;

/**
 * Port to the natural-language finance assistant model. Kept behind an interface so the service can
 * enforce its safety rules (prompt-injection scanning, tenant-scoped context, no autonomous
 * actions) regardless of the concrete model, and so the service degrades gracefully when no model
 * is wired.
 *
 * <p>Implementations MUST answer only from the tenant-scoped context they are given and MUST NOT be
 * able to trigger any state change (filing, posting, payment) — the assistant is advisory only.
 */
public interface AiAssistantPort {

  record Answer(String text, double confidence, boolean modelAvailable) {}

  boolean isAvailable();

  /**
   * @param tenantId the current tenant (for the model's own isolation/attribution)
   * @param sanitizedQuestion the user's question after prompt-injection neutralisation
   * @param tenantScopedContext facts assembled ONLY from this tenant's data
   */
  Answer answer(String tenantId, String sanitizedQuestion, String tenantScopedContext);
}

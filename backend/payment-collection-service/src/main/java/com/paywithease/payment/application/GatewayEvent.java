package com.paywithease.payment.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Canonical inbound webhook payload our gateway adapter posts (provider-specific formats are
 * normalized to this shape by the adapter before HMAC signing). {@code amountMinor} is integer
 * paise; {@code status} is SUCCESS or FAILED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayEvent(
    String eventId,
    String reference,
    long amountMinor,
    String status,
    String providerPaymentId,
    Long timestamp) {}

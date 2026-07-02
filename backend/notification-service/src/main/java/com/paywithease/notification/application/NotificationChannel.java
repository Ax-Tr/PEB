package com.paywithease.notification.application;

import com.paywithease.notification.domain.Channel;

/**
 * A delivery provider for one {@link Channel}. {@code accepted} means the provider took the message
 * for delivery (status SENT) — NOT that it was delivered; delivery is confirmed later via a
 * provider receipt webhook (product rule: never claim delivered without acknowledgement).
 */
public interface NotificationChannel {

  Channel channel();

  Result send(String recipient, String subject, String body);

  record Result(boolean accepted, String provider, String providerRef, String failureReason) {
    public static Result accepted(String provider, String providerRef) {
      return new Result(true, provider, providerRef, null);
    }

    public static Result rejected(String provider, String reason) {
      return new Result(false, provider, null, reason);
    }
  }
}

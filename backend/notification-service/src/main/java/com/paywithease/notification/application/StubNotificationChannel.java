package com.paywithease.notification.application;

import com.paywithease.common.ids.Ulid;
import com.paywithease.notification.domain.Channel;

/**
 * Development channel provider. Returns an accepted result with a synthetic provider reference.
 * Real providers (SMS gateway, SMTP/ESP, FCM/APNs push, WhatsApp BSP) replace these per
 * environment. {@code accept} is configurable so retry/failure paths can be exercised in tests.
 */
public class StubNotificationChannel implements NotificationChannel {

  private final Channel channel;
  private final String provider;
  private final boolean accept;

  public StubNotificationChannel(Channel channel, String provider, boolean accept) {
    this.channel = channel;
    this.provider = provider;
    this.accept = accept;
  }

  @Override
  public Channel channel() {
    return channel;
  }

  @Override
  public Result send(String recipient, String subject, String body) {
    return accept
        ? Result.accepted(provider, provider + "_" + Ulid.newId())
        : Result.rejected(provider, provider + " unavailable");
  }
}

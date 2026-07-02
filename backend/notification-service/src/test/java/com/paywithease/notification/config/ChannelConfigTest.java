package com.paywithease.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywithease.notification.application.NotificationChannel;
import com.paywithease.notification.domain.Channel;
import org.junit.jupiter.api.Test;

/** Verifies each dev channel bean is wired to the correct {@link Channel}. */
class ChannelConfigTest {

  private final ChannelConfig config = new ChannelConfig();

  @Test
  void smsBeanRoutesToSms() {
    NotificationChannel c = config.primarySms();
    assertThat(c.channel()).isEqualTo(Channel.SMS);
  }

  @Test
  void emailBeanRoutesToEmail() {
    NotificationChannel c = config.emailChannel();
    assertThat(c.channel()).isEqualTo(Channel.EMAIL);
  }

  @Test
  void pushBeanRoutesToPush() {
    NotificationChannel c = config.pushChannel();
    assertThat(c.channel()).isEqualTo(Channel.PUSH);
  }

  @Test
  void whatsappBeanRoutesToWhatsapp() {
    NotificationChannel c = config.whatsappChannel();
    assertThat(c.channel()).isEqualTo(Channel.WHATSAPP);
  }
}

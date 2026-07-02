package com.paywithease.notification.config;

import com.paywithease.notification.application.NotificationChannel;
import com.paywithease.notification.application.StubNotificationChannel;
import com.paywithease.notification.domain.Channel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a development provider per {@link Channel}. Real environments replace these stubs with
 * live gateway adapters (SMS gateway, SMTP/ESP, FCM/APNs push, WhatsApp BSP). {@link
 * com.paywithease.notification.application.ChannelRouter} autowires the resulting bean list.
 */
@Configuration
public class ChannelConfig {

  @Bean
  public NotificationChannel primarySms() {
    return new StubNotificationChannel(Channel.SMS, "smsprovider", true);
  }

  @Bean
  public NotificationChannel emailChannel() {
    return new StubNotificationChannel(Channel.EMAIL, "smtp", true);
  }

  @Bean
  public NotificationChannel pushChannel() {
    return new StubNotificationChannel(Channel.PUSH, "fcm", true);
  }

  @Bean
  public NotificationChannel whatsappChannel() {
    return new StubNotificationChannel(Channel.WHATSAPP, "whatsapp-bsp", true);
  }
}

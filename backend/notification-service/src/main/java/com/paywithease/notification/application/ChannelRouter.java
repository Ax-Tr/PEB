package com.paywithease.notification.application;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.notification.domain.Channel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Routes a message to the provider registered for its channel. */
@Component
public class ChannelRouter {

  private final Map<Channel, NotificationChannel> byChannel = new EnumMap<>(Channel.class);

  public ChannelRouter(List<NotificationChannel> channels) {
    for (NotificationChannel c : channels) {
      byChannel.put(c.channel(), c);
    }
  }

  public NotificationChannel forChannel(Channel channel) {
    NotificationChannel c = byChannel.get(channel);
    if (c == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "No provider for channel " + channel);
    }
    return c;
  }
}

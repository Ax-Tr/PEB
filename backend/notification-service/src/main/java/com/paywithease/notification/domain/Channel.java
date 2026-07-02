package com.paywithease.notification.domain;

/**
 * Delivery channels. WhatsApp is wired as a channel now; live BSP integration is gated on approval.
 */
public enum Channel {
  SMS,
  EMAIL,
  PUSH,
  WHATSAPP;

  public static boolean isValid(String value) {
    for (Channel c : values()) {
      if (c.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}

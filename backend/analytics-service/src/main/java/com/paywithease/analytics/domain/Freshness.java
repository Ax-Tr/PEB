package com.paywithease.analytics.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure freshness/staleness indicator for an event-fed read-model. Analytics is eventually
 * consistent, so every dashboard must be able to show how current its data is. Given when a stream
 * was last processed and the current time, this reports the lag and whether the read-model is stale
 * against a threshold.
 */
public final class Freshness {

  private Freshness() {}

  public enum State {
    FRESH,
    STALE,
    NO_DATA
  }

  public record Status(State state, long lagSeconds, Instant lastProcessedAt) {}

  public static Status evaluate(Instant lastProcessedAt, Instant now, Duration staleThreshold) {
    if (lastProcessedAt == null) {
      return new Status(State.NO_DATA, -1, null);
    }
    long lag = Math.max(0, Duration.between(lastProcessedAt, now).getSeconds());
    State state = lag > staleThreshold.getSeconds() ? State.STALE : State.FRESH;
    return new Status(state, lag, lastProcessedAt);
  }
}

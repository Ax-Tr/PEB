package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Per-(tenant, stream) freshness watermark: when the read-model was last advanced by an event. */
@Entity
@Table(name = "stream_watermarks")
public class StreamWatermark {

  @Id private String id; // tenant_id + '|' + stream

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String stream;

  @Column(name = "last_event_id", length = 26)
  private String lastEventId;

  @Column(name = "last_processed_at", nullable = false)
  private Instant lastProcessedAt;

  @Column(name = "events_processed", nullable = false)
  private long eventsProcessed;

  protected StreamWatermark() {}

  public StreamWatermark(String tenantId, String stream, String lastEventId, Instant now) {
    this.id = tenantId + "|" + stream;
    this.tenantId = tenantId;
    this.stream = stream;
    this.lastEventId = lastEventId;
    this.lastProcessedAt = now;
    this.eventsProcessed = 1;
  }

  public static String idOf(String tenantId, String stream) {
    return tenantId + "|" + stream;
  }

  public void advance(String lastEventId, Instant now) {
    this.lastEventId = lastEventId;
    this.lastProcessedAt = now;
    this.eventsProcessed++;
  }

  public String getStream() {
    return stream;
  }

  public Instant getLastProcessedAt() {
    return lastProcessedAt;
  }

  public long getEventsProcessed() {
    return eventsProcessed;
  }
}

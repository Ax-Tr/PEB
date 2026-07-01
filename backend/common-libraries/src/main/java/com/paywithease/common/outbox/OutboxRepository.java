package com.paywithease.common.outbox;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

  /**
   * Claims a batch of unpublished events with {@code FOR UPDATE SKIP LOCKED} so multiple relay
   * instances can run concurrently without processing the same row twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(
      @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query("select o from OutboxEvent o where o.publishedAt is null order by o.createdAt asc")
  List<OutboxEvent> claimUnpublished(Limit limit);
}

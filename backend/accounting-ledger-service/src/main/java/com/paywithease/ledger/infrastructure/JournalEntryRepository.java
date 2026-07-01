package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.JournalEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {
  boolean existsByTenantIdAndSourceServiceAndSourceEventId(
      String tenantId, String sourceService, String sourceEventId);

  Optional<JournalEntry> findByTenantIdAndSourceServiceAndSourceEventId(
      String tenantId, String sourceService, String sourceEventId);

  Optional<JournalEntry> findByTenantIdAndId(String tenantId, String id);

  // True append-only: an entry is "reversed" iff a reversing entry points at it (we never mutate
  // it).
  boolean existsByTenantIdAndReversalOf(String tenantId, String reversalOf);
}

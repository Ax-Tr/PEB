package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.JournalEntryLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, String> {
  List<JournalEntryLine> findByJournalEntryId(String journalEntryId);

  List<JournalEntryLine> findByTenantIdAndAccountIdOrderById(String tenantId, String accountId);
}

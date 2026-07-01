package com.paywithease.invoice.application;

import com.paywithease.invoice.domain.DocumentSequence;
import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.infrastructure.DocumentSequenceRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates gap-free, per-tenant/per-financial-year document numbers of the form {@code
 * PREFIX/2026-27/00001}. Concurrency is serialized by pessimistically locking the sequence row.
 */
@Service
public class InvoiceNumberService {

  private final DocumentSequenceRepository sequences;
  private final Clock clock;

  public InvoiceNumberService(DocumentSequenceRepository sequences, Clock clock) {
    this.sequences = sequences;
    this.clock = clock;
  }

  /** Indian financial year (Apr–Mar), e.g. a date in May 2026 → "2026-27"; Feb 2026 → "2025-26". */
  public String financialYear(LocalDate date) {
    int year = date.getYear();
    int startYear = date.getMonthValue() >= 4 ? year : year - 1;
    int endYear = startYear + 1;
    return String.format("%04d-%02d", startYear, endYear % 100);
  }

  @Transactional
  public String next(String tenantId, DocumentType docType, String prefix, LocalDate date) {
    String fy = financialYear(date);
    String docTypeName = docType.name();
    DocumentSequence seq = loadOrCreate(tenantId, docTypeName, fy, prefix);
    long n = seq.allocate();
    sequences.save(seq);
    return prefix + "/" + fy + "/" + String.format("%05d", n);
  }

  private DocumentSequence loadOrCreate(
      String tenantId, String docTypeName, String fy, String prefix) {
    return sequences
        .lock(tenantId, docTypeName, fy)
        .orElseGet(
            () -> {
              try {
                DocumentSequence created =
                    new DocumentSequence(tenantId, docTypeName, fy, prefix, 1L);
                return sequences.save(created);
              } catch (DataIntegrityViolationException concurrentCreate) {
                // Another thread created the row first — re-lock and use it.
                return sequences
                    .lock(tenantId, docTypeName, fy)
                    .orElseThrow(() -> concurrentCreate);
              }
            });
  }
}

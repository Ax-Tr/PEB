package com.paywithease.invoice.infrastructure;

import com.paywithease.invoice.domain.DocumentSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentSequenceRepository
    extends JpaRepository<DocumentSequence, DocumentSequence.PK> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from DocumentSequence s where s.tenantId=:t and s.docType=:d and s.financialYear=:fy")
  Optional<DocumentSequence> lock(
      @Param("t") String t, @Param("d") String d, @Param("fy") String fy);
}

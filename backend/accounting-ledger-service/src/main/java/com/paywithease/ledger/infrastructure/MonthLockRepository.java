package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.MonthLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthLockRepository extends JpaRepository<MonthLock, String> {}

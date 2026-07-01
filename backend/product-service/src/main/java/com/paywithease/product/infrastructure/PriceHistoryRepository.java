package com.paywithease.product.infrastructure;

import com.paywithease.product.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, String> {}

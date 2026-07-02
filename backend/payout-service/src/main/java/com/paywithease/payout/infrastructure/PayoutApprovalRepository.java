package com.paywithease.payout.infrastructure;

import com.paywithease.payout.domain.PayoutApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutApprovalRepository extends JpaRepository<PayoutApproval, String> {}

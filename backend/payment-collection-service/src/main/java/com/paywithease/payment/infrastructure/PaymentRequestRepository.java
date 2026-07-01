package com.paywithease.payment.infrastructure;

import com.paywithease.payment.domain.PaymentRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, String> {
  // reference embeds a ULID so it is effectively globally unique; used to resolve inbound webhooks.
  Optional<PaymentRequest> findByReference(String reference);
}

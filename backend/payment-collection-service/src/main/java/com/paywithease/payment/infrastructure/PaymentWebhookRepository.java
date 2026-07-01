package com.paywithease.payment.infrastructure;

import com.paywithease.payment.domain.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, String> {
  boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}

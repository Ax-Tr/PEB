package com.paywithease.payment.infrastructure;

import com.paywithease.payment.domain.PaymentQrCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentQrCodeRepository extends JpaRepository<PaymentQrCode, String> {
  Optional<PaymentQrCode> findByPaymentRequestId(String paymentRequestId);
}

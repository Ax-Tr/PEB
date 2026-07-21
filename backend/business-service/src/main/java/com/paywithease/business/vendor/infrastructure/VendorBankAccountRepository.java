package com.paywithease.business.vendor.infrastructure;

import com.paywithease.business.vendor.domain.VendorBankAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, String> {
  boolean existsByVendorIdAndAccountNumberHash(String vendorId, String accountNumberHash);

  List<VendorBankAccount> findByVendorId(String vendorId);
}

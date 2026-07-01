package com.paywithease.vendor.infrastructure;

import com.paywithease.vendor.domain.VendorBankAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, String> {
  List<VendorBankAccount> findByVendorId(String vendorId);

  boolean existsByVendorIdAndAccountNumberHash(String vendorId, String accountNumberHash);
}

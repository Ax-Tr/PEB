package com.paywithease.finance;

import com.paywithease.common.security.BlindIndex;
import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.PartyType;
import com.paywithease.payout.infrastructure.BeneficiaryRepository;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Developer utility to automatically seed matching beneficiaries in finance_db so they align with
 * the business-service seeded vendors, allowing payouts to proceed.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

  private final BeneficiaryRepository beneficiaries;
  private final BlindIndex blindIndex;

  public DevDataSeeder(BeneficiaryRepository beneficiaries, BlindIndex blindIndex) {
    this.beneficiaries = beneficiaries;
    this.blindIndex = blindIndex;
  }

  @Override
  public void run(String... args) throws Exception {
    Instant now = Instant.now();

    // Tenant 1 Beneficiaries
    String t1 = "01KY2D1JC2V07VXZFRR0DBTFZ5";
    seedBeneficiary(
        t1,
        "01KY4WDM0HBZ9E59K2JCZHP8N8", // Beneficiary/Bank Account ID (matches business-service)
        "01KY4WDKPVXSJ87BMCR76T7MQK", // Vendor ID (matches business-service)
        "Sri Kanya",
        "50100123456789",
        now);
    seedBeneficiary(
        t1,
        "01KY4WDM4AZW9NR6E64DTJP3GE", // Beneficiary/Bank Account ID (matches business-service)
        "01KY4WDM33JC1GH5EJD39GH4NW", // Vendor ID (matches business-service)
        "SVR Logistics",
        "912345678901",
        now);

    // Tenant 2 Beneficiaries
    String t2 = "01KY2D81VX3G18N5ASMHVYQCNG";
    seedBeneficiary(
        t2,
        "01KY4WDM7DN9EMMXZTHGYFRPY4", // Beneficiary/Bank Account ID (matches business-service)
        "01KY4WDM6017RTD4SE45VKPFS4", // Vendor ID (matches business-service)
        "Sri Kanya",
        "50100123456789",
        now);
    seedBeneficiary(
        t2,
        "01KY4WDMA5WPTHNCP39AX11Y10", // Beneficiary/Bank Account ID (matches business-service)
        "01KY4WDM8ZWTT15XFP6A8RVBJP", // Vendor ID (matches business-service)
        "SVR Logistics",
        "912345678901",
        now);
  }

  private void seedBeneficiary(
      String tenantId,
      String beneficiaryId,
      String vendorId,
      String name,
      String accountNumber,
      Instant now) {
    try {
      if (!beneficiaries.existsById(beneficiaryId)) {
        Beneficiary b =
            new Beneficiary(
                beneficiaryId,
                tenantId,
                PartyType.VENDOR,
                vendorId,
                name,
                blindIndex.hash(accountNumber),
                now, // verifiedAt (must be non-null to not be high risk)
                now // createdAt
                );
        beneficiaries.save(b);
      }
    } catch (Exception e) {
      System.err.println(
          "DevDataSeeder error for beneficiary " + beneficiaryId + ": " + e.getMessage());
    }
  }
}

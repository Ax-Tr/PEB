package com.paywithease.business;

import com.paywithease.common.security.BlindIndex;
import com.paywithease.vendor.domain.BankAccountSource;
import com.paywithease.vendor.domain.Vendor;
import com.paywithease.vendor.domain.VendorBankAccount;
import com.paywithease.vendor.infrastructure.VendorBankAccountRepository;
import com.paywithease.vendor.infrastructure.VendorRepository;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Developer utility to automatically seed vendors and verified bank accounts in the database for
 * default local tenants with hardcoded ULIDs, ensuring they align with downstream beneficiaries.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

  private final VendorRepository vendors;
  private final VendorBankAccountRepository bankAccounts;
  private final BlindIndex blindIndex;

  public DevDataSeeder(
      VendorRepository vendors, VendorBankAccountRepository bankAccounts, BlindIndex blindIndex) {
    this.vendors = vendors;
    this.bankAccounts = bankAccounts;
    this.blindIndex = blindIndex;
  }

  @Override
  public void run(String... args) throws Exception {
    Instant now = Instant.now();

    // Tenant 1 Seeding
    String t1 = "01KY2D1JC2V07VXZFRR0DBTFZ5";
    seedTenantVendors(
        t1,
        "01KY4WDKPVXSJ87BMCR76T7MQK", // Sri Kanya Vendor ID
        "01KY4WDM0HBZ9E59K2JCZHP8N8", // Sri Kanya Bank Account ID
        "01KY4WDM33JC1GH5EJD39GH4NW", // SVR Logistics Vendor ID
        "01KY4WDM4AZW9NR6E64DTJP3GE", // SVR Logistics Bank Account ID
        now);

    // Tenant 2 Seeding
    String t2 = "01KY2D81VX3G18N5ASMHVYQCNG";
    seedTenantVendors(
        t2,
        "01KY4WDM6017RTD4SE45VKPFS4", // Sri Kanya Vendor ID
        "01KY4WDM7DN9EMMXZTHGYFRPY4", // Sri Kanya Bank Account ID
        "01KY4WDM8ZWTT15XFP6A8RVBJP", // SVR Logistics Vendor ID
        "01KY4WDMA5WPTHNCP39AX11Y10", // SVR Logistics Bank Account ID
        now);
  }

  private void seedTenantVendors(
      String tenantId, String v1Id, String ba1Id, String v2Id, String ba2Id, Instant now) {
    try {
      // 1. Sri Kanya
      if (!vendors.existsById(v1Id)) {
        Vendor v1 = new Vendor(v1Id, tenantId, "Sri Kanya", now);
        v1.setMobile("+919876543210");
        v1.setEmail("srikanya@example.com");
        v1.setGstin("27AAAAA1111A1Z1");
        v1.setAddress("Visakhapatnam, AP");
        vendors.save(v1);

        String acc1 = "50100123456789";
        VendorBankAccount ba1 =
            new VendorBankAccount(
                ba1Id,
                tenantId,
                v1Id,
                acc1,
                blindIndex.hash(acc1),
                "HDFC0001234",
                "srikanya@upi",
                "HDFC Bank",
                "Sri Kanya Enterprises",
                BankAccountSource.MANUAL,
                now);
        ba1.confirm("SYSTEM", now);
        bankAccounts.save(ba1);
      }

      // 2. SVR Logistics
      if (!vendors.existsById(v2Id)) {
        Vendor v2 = new Vendor(v2Id, tenantId, "SVR Logistics", now);
        v2.setMobile("+919876543211");
        v2.setEmail("svr@example.com");
        v2.setGstin("27BBBBB2222B2Z2");
        v2.setAddress("Hyderabad, TS");
        vendors.save(v2);

        String acc2 = "912345678901";
        VendorBankAccount ba2 =
            new VendorBankAccount(
                ba2Id,
                tenantId,
                v2Id,
                acc2,
                blindIndex.hash(acc2),
                "SBIN0004321",
                "svr@upi",
                "State Bank of India",
                "SVR Logistics Pvt Ltd",
                BankAccountSource.MANUAL,
                now);
        ba2.confirm("SYSTEM", now);
        bankAccounts.save(ba2);
      }
    } catch (Exception e) {
      System.err.println("DevDataSeeder error for tenant " + tenantId + ": " + e.getMessage());
    }
  }
}

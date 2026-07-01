package com.paywithease.vendor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.vendor.domain.BankAccountSource;
import com.paywithease.vendor.domain.BankAccountStatus;
import com.paywithease.vendor.domain.Vendor;
import com.paywithease.vendor.domain.VendorBankAccount;
import com.paywithease.vendor.infrastructure.VendorBankAccountRepository;
import com.paywithease.vendor.infrastructure.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

  @Mock private VendorRepository vendors;
  @Mock private VendorBankAccountRepository bankAccounts;
  @Mock private AuditWriter audit;
  @Mock private OutboxWriter outbox;

  private final BlindIndex blindIndex = new BlindIndex(new byte[32]);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  private VendorService service;

  @BeforeEach
  void setUp() {
    service =
        new VendorService(vendors, bankAccounts, blindIndex, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createVendorEmitsEvent() {
    when(vendors.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

    Vendor vendor = service.create("Acme Supplies", "+919876543210", "a@b.com", null, null);

    assertThat(vendor.getTenantId()).isEqualTo("tenant1");
    assertThat(vendor.getName()).isEqualTo("Acme Supplies");
    verify(audit).record(eq("VENDOR_CREATED"), eq("vendor"), anyString(), any());

    ArgumentCaptor<EventEnvelope> envelope = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(envelope.capture());
    assertThat(envelope.getValue().eventType()).isEqualTo("VENDOR_CREATED");
    assertThat(envelope.getValue().tenantId()).isEqualTo("tenant1");
  }

  @Test
  void addBankAccountIsPendingReviewAndEmitsNoDetailsEvent() {
    Vendor vendor = new Vendor("vendorX", "tenant1", "Acme", clock.instant());
    when(vendors.findById("vendorX")).thenReturn(Optional.of(vendor));
    when(bankAccounts.existsByVendorIdAndAccountNumberHash(anyString(), anyString()))
        .thenReturn(false);
    when(bankAccounts.save(any(VendorBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

    VendorBankAccount account =
        service.addBankAccount(
            "vendorX", "123456789012", "HDFC0001234", null, "HDFC Bank", "Acme Owner", "OCR");

    assertThat(account.getStatus()).isEqualTo(BankAccountStatus.PENDING_REVIEW);
    assertThat(account.getSource()).isEqualTo(BankAccountSource.OCR);

    ArgumentCaptor<VendorBankAccount> saved = ArgumentCaptor.forClass(VendorBankAccount.class);
    verify(bankAccounts).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(BankAccountStatus.PENDING_REVIEW);

    // Capture is not a usable state change: no domain event may be emitted yet.
    verify(outbox, never()).append(any());
  }

  @Test
  void confirmBankAccountVerifiesAndEmitsEvent() {
    VendorBankAccount account =
        new VendorBankAccount(
            "ba1",
            "tenant1",
            "vendorX",
            "123456789012",
            blindIndex.hash("123456789012"),
            "HDFC0001234",
            null,
            "HDFC Bank",
            "Acme Owner",
            BankAccountSource.OCR,
            clock.instant());
    when(bankAccounts.findById("ba1")).thenReturn(Optional.of(account));
    when(bankAccounts.save(any(VendorBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

    VendorBankAccount confirmed = service.confirmBankAccount("vendorX", "ba1", "reviewer1");

    assertThat(confirmed.getStatus()).isEqualTo(BankAccountStatus.VERIFIED);
    assertThat(confirmed.getReviewedBy()).isEqualTo("reviewer1");
    verify(audit)
        .record(eq("VENDOR_BANK_ACCOUNT_VERIFIED"), eq("vendor_bank_account"), eq("ba1"), any());

    ArgumentCaptor<EventEnvelope> envelope = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox, times(1)).append(envelope.capture());
    assertThat(envelope.getValue().eventType()).isEqualTo("VENDOR_BANK_DETAILS_CHANGED");
  }

  @Test
  void confirmRejectsNonPending() {
    VendorBankAccount account =
        new VendorBankAccount(
            "ba1",
            "tenant1",
            "vendorX",
            "123456789012",
            blindIndex.hash("123456789012"),
            "HDFC0001234",
            null,
            "HDFC Bank",
            "Acme Owner",
            BankAccountSource.MANUAL,
            clock.instant());
    account.confirm("reviewer1", clock.instant()); // already VERIFIED
    when(bankAccounts.findById("ba1")).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.confirmBankAccount("vendorX", "ba1", "reviewer2"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("not pending review");

    verify(outbox, never()).append(any());
  }
}

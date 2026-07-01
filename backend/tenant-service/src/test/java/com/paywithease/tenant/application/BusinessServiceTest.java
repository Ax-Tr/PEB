package com.paywithease.tenant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.tenant.domain.Business;
import com.paywithease.tenant.domain.Gstin;
import com.paywithease.tenant.infrastructure.BranchRepository;
import com.paywithease.tenant.infrastructure.BusinessRepository;
import com.paywithease.tenant.infrastructure.SettingsRepository;
import com.paywithease.tenant.infrastructure.TaxProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

  @Mock BusinessRepository businesses;
  @Mock BranchRepository branches;
  @Mock TaxProfileRepository taxProfiles;
  @Mock SettingsRepository settings;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private BusinessService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new BusinessService(
            businesses,
            branches,
            taxProfiles,
            settings,
            new BlindIndex(new byte[32]),
            audit,
            outbox,
            new ObjectMapper(),
            clock);
  }

  @Test
  void createSeedsProfileTaxAndSettingsAndEmitsEvent() {
    when(businesses.save(any(Business.class))).thenAnswer(returnsFirstArg());

    Business b = service.create("owner1", "Acme Traders", "Acme", "PROPRIETOR", "27");

    assertThat(b.getOwnerUserId()).isEqualTo("owner1");
    assertThat(b.getBusinessType()).isEqualTo("PROPRIETOR");
    assertThat(b.getTradeName()).isEqualTo("Acme");
    verify(taxProfiles).save(any());
    verify(settings).save(any());
    verify(outbox).append(any(EventEnvelope.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void createRejectsUnknownBusinessType() {
    assertThatThrownBy(() -> service.create("o", "n", "t", "BOGUS", "27"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("businessType");
  }

  @Test
  void setTaxIdentifiersValidatesAndStores() {
    Business existing = new Business("biz1", "owner1", "Acme", "PROPRIETOR", "27", clock.instant());
    when(businesses.findById("biz1")).thenReturn(Optional.of(existing));
    when(businesses.existsByGstinHash(any())).thenReturn(false);

    String first14 = "27AAPFU0939F1Z";
    String gstin = first14 + Gstin.checkDigit(first14);

    Business updated =
        service.setTaxIdentifiers("biz1", gstin, "AAPFU0939F", "UDYAM-MH-01-1234567");

    assertThat(updated.getGstin()).isEqualTo(gstin);
    assertThat(updated.getPan()).isEqualTo("AAPFU0939F");
    verify(outbox).append(any(EventEnvelope.class));
  }

  @Test
  void setTaxIdentifiersRejectsInvalidGstin() {
    Business existing = new Business("biz1", "owner1", "Acme", "PROPRIETOR", "27", clock.instant());
    when(businesses.findById("biz1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.setTaxIdentifiers("biz1", "27AAPFU0939F1ZZ", null, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Invalid GSTIN");
  }

  @Test
  void setTaxIdentifiersRejectsDuplicateGstin() {
    Business existing = new Business("biz1", "owner1", "Acme", "PROPRIETOR", "27", clock.instant());
    when(businesses.findById("biz1")).thenReturn(Optional.of(existing));
    when(businesses.existsByGstinHash(any())).thenReturn(true); // another business already has it

    String first14 = "27AAPFU0939F1Z";
    String gstin = first14 + Gstin.checkDigit(first14);

    assertThatThrownBy(() -> service.setTaxIdentifiers("biz1", gstin, null, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already registered");
  }
}

package com.paywithease.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoaSeederTest {

  @Mock ChartOfAccountRepository accounts;
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void seedsStandardChartWhenAbsent() {
    when(accounts.existsByTenantId("tenant1")).thenReturn(false);
    CoaSeeder seeder = new CoaSeeder(accounts, clock);
    int created = seeder.seedIfAbsent("tenant1");
    assertThat(created).isEqualTo(24);
    verify(accounts).saveAll(anyList());
  }

  @Test
  void skipsWhenAlreadySeeded() {
    when(accounts.existsByTenantId("tenant1")).thenReturn(true);
    CoaSeeder seeder = new CoaSeeder(accounts, clock);
    assertThat(seeder.seedIfAbsent("tenant1")).isZero();
    verify(accounts, never()).saveAll(anyList());
  }
}

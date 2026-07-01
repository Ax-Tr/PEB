package com.paywithease.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.customer.domain.Customer;
import com.paywithease.customer.infrastructure.CustomerContactRepository;
import com.paywithease.customer.infrastructure.CustomerRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

  @Mock CustomerRepository customers;
  @Mock CustomerContactRepository contacts;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private CustomerService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new CustomerService(
            customers,
            contacts,
            new BlindIndex(new byte[32]),
            audit,
            outbox,
            new ObjectMapper(),
            clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createPersistsAndEmitsEvent() {
    when(customers.existsByTenantIdAndMobileHash(eq("tenant1"), any())).thenReturn(false);
    when(customers.save(any(Customer.class))).thenAnswer(returnsFirstArg());

    Customer c = service.create("Ravi Kumar", "9876543210", "ravi@example.com", "MG Road", null);

    assertThat(c.getTenantId()).isEqualTo("tenant1");
    assertThat(c.getName()).isEqualTo("Ravi Kumar");
    assertThat(c.getMobile()).isEqualTo("9876543210");
    assertThat(c.getEmail()).isEqualTo("ravi@example.com");
    verify(customers).save(any(Customer.class));
    verify(outbox).append(any(EventEnvelope.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void createRejectsDuplicateMobile() {
    when(customers.existsByTenantIdAndMobileHash(eq("tenant1"), any())).thenReturn(true);

    assertThatThrownBy(() -> service.create("Ravi Kumar", "9876543210", null, null, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void searchByMobileReturnsMatch() {
    Customer existing =
        new Customer(
            "cust1",
            "tenant1",
            "Ravi Kumar",
            "9876543210",
            new BlindIndex(new byte[32]).hash("9876543210"),
            clock.instant());
    when(customers.findByTenantIdAndMobileHash(eq("tenant1"), any()))
        .thenReturn(Optional.of(existing));

    Optional<Customer> found = service.searchByMobile("+91 98765-43210");

    assertThat(found).contains(existing);
  }
}

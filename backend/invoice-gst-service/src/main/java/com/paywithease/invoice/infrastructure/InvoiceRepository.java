package com.paywithease.invoice.infrastructure;

import com.paywithease.invoice.domain.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

  Optional<Invoice> findByTenantIdAndId(String tenantId, String id);

  List<Invoice> findByTenantIdAndInvoiceDateBetweenOrderByInvoiceDateAsc(
      String tenantId, LocalDate from, LocalDate to);
}

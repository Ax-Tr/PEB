package com.paywithease.invoice.infrastructure;

import com.paywithease.invoice.domain.InvoiceItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {

  List<InvoiceItem> findByInvoiceId(String invoiceId);
}

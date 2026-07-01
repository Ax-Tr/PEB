package com.paywithease.invoice.infrastructure;

import com.paywithease.invoice.domain.GstTaxLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GstTaxLineRepository extends JpaRepository<GstTaxLine, String> {

  List<GstTaxLine> findByInvoiceId(String invoiceId);
}

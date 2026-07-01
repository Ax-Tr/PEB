package com.paywithease.invoice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.money.Money;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds a representative e-way-bill JSON payload in READINESS-ONLY mode. Transport/distance
 * details are out of scope at readiness; it reports the document-level fields required to generate
 * an EWB.
 */
@Component
public class EwayPayloadBuilder {

  private final ObjectMapper objectMapper;

  public EwayPayloadBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Readiness build(Invoice inv, List<InvoiceItem> items) {
    List<String> missing = new ArrayList<>();
    if (isBlank(inv.getInvoiceNumber())) {
      missing.add("docNo");
    }
    if (isBlank(inv.getBusinessStateCode())) {
      missing.add("fromStateCode");
    }

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("status", "READINESS_NOT_FILED");
    // Outward supply for all issued documents at readiness stage.
    payload.put("supplyType", "O");
    payload.put("subSupplyType", "1");
    payload.put("docType", mapDocType(inv.getDocumentType()));
    payload.put("docNo", inv.getInvoiceNumber());
    payload.put("docDate", inv.getInvoiceDate() == null ? null : inv.getInvoiceDate().toString());
    payload.put("fromGstin", nullSafe(null));
    payload.put("fromStateCode", inv.getBusinessStateCode());
    payload.put("toGstin", nullSafe(inv.getCustomerGstin()));
    payload.put("toStateCode", inv.getPlaceOfSupply());
    payload.put("totalValue", Money.ofMinor(inv.getTotalAmountMinor()).toRupees());
    payload.put("cgstValue", Money.ofMinor(inv.getTotalCgstMinor()).toRupees());
    payload.put("sgstValue", Money.ofMinor(inv.getTotalSgstMinor()).toRupees());
    payload.put("igstValue", Money.ofMinor(inv.getTotalIgstMinor()).toRupees());

    ArrayNode itemList = payload.putArray("itemList");
    for (InvoiceItem item : items) {
      ObjectNode node = itemList.addObject();
      node.put("productName", item.getDescription());
      node.put("hsnCode", nullSafe(item.getHsnSac()));
      node.put("quantity", item.getQuantity() == null ? null : item.getQuantity().toPlainString());
      node.put("taxableAmount", Money.ofMinor(item.getTaxableValueMinor()).toRupees());
      node.put("totalValue", Money.ofMinor(item.getLineTotalMinor()).toRupees());
    }

    return new Readiness(missing.isEmpty(), missing, payload);
  }

  private static String mapDocType(String documentType) {
    return switch (documentType) {
      case "CREDIT_NOTE" -> "CRN";
      case "DEBIT_NOTE" -> "DBN";
      case "BILL_OF_SUPPLY" -> "BIL";
      default -> "INV";
    };
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }
}

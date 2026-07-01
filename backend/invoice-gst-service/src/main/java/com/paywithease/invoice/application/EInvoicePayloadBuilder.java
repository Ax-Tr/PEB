package com.paywithease.invoice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.money.Money;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import com.paywithease.invoice.domain.SupplyType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds a representative IRP (e-invoice) JSON payload in READINESS-ONLY mode. It never files with
 * the IRP; it validates that the data required for an IRN request is present and reports what is
 * missing.
 */
@Component
public class EInvoicePayloadBuilder {

  private final ObjectMapper objectMapper;

  public EInvoicePayloadBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Readiness build(Invoice inv, List<InvoiceItem> items) {
    List<String> missing = new ArrayList<>();

    if (SupplyType.B2B.name().equals(inv.getSupplyType()) && isBlank(inv.getCustomerGstin())) {
      missing.add("buyerGstin");
    }
    if (isBlank(inv.getBusinessStateCode())) {
      missing.add("sellerStateCode");
    }
    for (InvoiceItem item : items) {
      if (isBlank(item.getHsnSac())) {
        missing.add("item.hsnSac");
        break;
      }
    }

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("status", "READINESS_NOT_FILED");
    payload.put("Version", "1.1");

    ObjectNode tranDtls = payload.putObject("TranDtls");
    tranDtls.put("TaxSch", "GST");
    tranDtls.put("SupTyp", SupplyType.B2B.name().equals(inv.getSupplyType()) ? "B2B" : "B2C");
    tranDtls.put("RegRev", inv.isReverseCharge() ? "Y" : "N");

    ObjectNode docDtls = payload.putObject("DocDtls");
    docDtls.put("Typ", mapDocType(inv.getDocumentType()));
    docDtls.put("No", inv.getInvoiceNumber());
    docDtls.put("Dt", inv.getInvoiceDate() == null ? null : inv.getInvoiceDate().toString());

    ObjectNode sellerDtls = payload.putObject("SellerDtls");
    sellerDtls.put("Gstin", nullSafe(null));
    sellerDtls.put("StateCd", inv.getBusinessStateCode());

    ObjectNode buyerDtls = payload.putObject("BuyerDtls");
    buyerDtls.put("Gstin", nullSafe(inv.getCustomerGstin()));
    buyerDtls.put("StateCd", inv.getPlaceOfSupply());

    ArrayNode itemList = payload.putArray("ItemList");
    int slNo = 1;
    for (InvoiceItem item : items) {
      ObjectNode node = itemList.addObject();
      node.put("SlNo", String.valueOf(slNo++));
      node.put("HsnCd", nullSafe(item.getHsnSac()));
      node.put("Qty", item.getQuantity() == null ? null : item.getQuantity().toPlainString());
      node.put("AssAmt", Money.ofMinor(item.getTaxableValueMinor()).toRupees());
      node.put("GstRt", item.getGstRate());
      node.put("CgstAmt", Money.ofMinor(item.getCgstMinor()).toRupees());
      node.put("SgstAmt", Money.ofMinor(item.getSgstMinor()).toRupees());
      node.put("IgstAmt", Money.ofMinor(item.getIgstMinor()).toRupees());
      node.put("TotItemVal", Money.ofMinor(item.getLineTotalMinor()).toRupees());
    }

    ObjectNode valDtls = payload.putObject("ValDtls");
    valDtls.put("AssVal", Money.ofMinor(inv.getTotalTaxableMinor()).toRupees());
    valDtls.put("CgstVal", Money.ofMinor(inv.getTotalCgstMinor()).toRupees());
    valDtls.put("SgstVal", Money.ofMinor(inv.getTotalSgstMinor()).toRupees());
    valDtls.put("IgstVal", Money.ofMinor(inv.getTotalIgstMinor()).toRupees());
    valDtls.put("TotInvVal", Money.ofMinor(inv.getTotalAmountMinor()).toRupees());

    return new Readiness(missing.isEmpty(), missing, payload);
  }

  private static String mapDocType(String documentType) {
    return switch (documentType) {
      case "CREDIT_NOTE" -> "CRN";
      case "DEBIT_NOTE" -> "DBN";
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

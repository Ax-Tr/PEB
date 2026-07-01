package com.paywithease.invoice.application;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.paywithease.common.money.Money;
import com.paywithease.invoice.domain.GstTaxLine;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Renders an invoice/note to a simple, self-contained A4 PDF using OpenPDF (built-in Helvetica, no
 * external fonts). Checked PDF exceptions are wrapped as {@link IllegalStateException}.
 */
@Component
public class InvoicePdfGenerator {

  private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
  private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
  private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
  private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 8);

  public byte[] generate(Invoice inv, List<InvoiceItem> items, List<GstTaxLine> taxLines) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    try {
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(
          new Paragraph(
              humanDocType(inv.getDocumentType()) + " " + safe(inv.getInvoiceNumber()), TITLE));
      document.add(new Paragraph(" ", NORMAL));

      document.add(line("Invoice Date: ", safe(String.valueOf(inv.getInvoiceDate()))));
      document.add(line("Financial Year: ", safe(inv.getFinancialYear())));
      document.add(line("Seller State Code: ", safe(inv.getBusinessStateCode())));
      document.add(line("Place of Supply: ", safe(inv.getPlaceOfSupply())));
      document.add(line("Buyer: ", safe(inv.getCustomerName())));
      document.add(line("Buyer GSTIN: ", safe(inv.getCustomerGstin())));
      document.add(line("Supply Type: ", safe(inv.getSupplyType())));
      if (inv.isReverseCharge()) {
        document.add(line("Reverse Charge: ", "Yes"));
      }
      if (inv.getOriginalDocumentId() != null && !inv.getOriginalDocumentId().isBlank()) {
        document.add(line("Against Document: ", inv.getOriginalDocumentId()));
      }
      document.add(new Paragraph(" ", NORMAL));

      PdfPTable table = new PdfPTable(new float[] {28, 10, 8, 12, 12, 8, 10, 12});
      table.setWidthPercentage(100);
      addHeader(
          table,
          "Description",
          "HSN/SAC",
          "Qty",
          "Unit Price",
          "Taxable",
          "GST%",
          "Tax",
          "Line Total");
      for (InvoiceItem item : items) {
        long lineTax = item.getCgstMinor() + item.getSgstMinor() + item.getIgstMinor();
        addCell(table, safe(item.getDescription()), Element.ALIGN_LEFT);
        addCell(table, safe(item.getHsnSac()), Element.ALIGN_LEFT);
        addCell(
            table,
            item.getQuantity() == null ? "" : item.getQuantity().toPlainString(),
            Element.ALIGN_RIGHT);
        addCell(table, Money.ofMinor(item.getUnitPriceMinor()).toString(), Element.ALIGN_RIGHT);
        addCell(table, Money.ofMinor(item.getTaxableValueMinor()).toString(), Element.ALIGN_RIGHT);
        addCell(
            table,
            item.getGstRate() == null ? "0" : item.getGstRate().toPlainString(),
            Element.ALIGN_RIGHT);
        addCell(table, Money.ofMinor(lineTax).toString(), Element.ALIGN_RIGHT);
        addCell(table, Money.ofMinor(item.getLineTotalMinor()).toString(), Element.ALIGN_RIGHT);
      }
      document.add(table);
      document.add(new Paragraph(" ", NORMAL));

      document.add(new Paragraph("Tax Summary", LABEL));
      PdfPTable summary = new PdfPTable(new float[] {20, 25, 20, 20, 20});
      summary.setWidthPercentage(100);
      addHeader(summary, "GST%", "Taxable", "CGST", "SGST", "IGST");
      for (GstTaxLine tl : taxLines) {
        addCell(
            summary,
            tl.getGstRate() == null ? "0" : tl.getGstRate().toPlainString(),
            Element.ALIGN_RIGHT);
        addCell(summary, Money.ofMinor(tl.getTaxableValueMinor()).toString(), Element.ALIGN_RIGHT);
        addCell(summary, Money.ofMinor(tl.getCgstMinor()).toString(), Element.ALIGN_RIGHT);
        addCell(summary, Money.ofMinor(tl.getSgstMinor()).toString(), Element.ALIGN_RIGHT);
        addCell(summary, Money.ofMinor(tl.getIgstMinor()).toString(), Element.ALIGN_RIGHT);
      }
      document.add(summary);
      document.add(new Paragraph(" ", NORMAL));

      document.add(line("Total Taxable: ", Money.ofMinor(inv.getTotalTaxableMinor()).toString()));
      document.add(line("Total CGST: ", Money.ofMinor(inv.getTotalCgstMinor()).toString()));
      document.add(line("Total SGST: ", Money.ofMinor(inv.getTotalSgstMinor()).toString()));
      document.add(line("Total IGST: ", Money.ofMinor(inv.getTotalIgstMinor()).toString()));
      document.add(line("Total Tax: ", Money.ofMinor(inv.getTotalTaxMinor()).toString()));
      Paragraph grand =
          new Paragraph(
              "Grand Total: " + Money.ofMinor(inv.getTotalAmountMinor()).toString(), LABEL);
      document.add(grand);

      document.close();
      return out.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException("Failed to render invoice PDF", e);
    }
  }

  private static Paragraph line(String label, String value) {
    Paragraph p = new Paragraph();
    p.add(new Phrase(label, LABEL));
    p.add(new Phrase(value, NORMAL));
    return p;
  }

  private static void addHeader(PdfPTable table, String... titles) {
    for (String title : titles) {
      PdfPCell cell = new PdfPCell(new Phrase(title, HEAD));
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      table.addCell(cell);
    }
  }

  private static void addCell(PdfPTable table, String text, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(text, CELL));
    cell.setHorizontalAlignment(alignment);
    table.addCell(cell);
  }

  private static String humanDocType(String documentType) {
    return documentType == null ? "" : documentType.replace('_', ' ');
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}

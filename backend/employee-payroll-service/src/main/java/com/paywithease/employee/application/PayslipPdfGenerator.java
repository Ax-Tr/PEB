package com.paywithease.employee.application;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfWriter;
import com.paywithease.common.money.Money;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;

/**
 * Renders an employee payslip to a simple, self-contained A4 PDF using OpenPDF (built-in Helvetica,
 * no external fonts). Checked PDF exceptions are wrapped as {@link IllegalStateException}.
 */
@Component
public class PayslipPdfGenerator {

  private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
  private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
  private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);

  public byte[] generate(SalaryRun run, SalaryRunLine line, String employeeName) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    try {
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(new Paragraph("Payslip", TITLE));
      document.add(new Paragraph(" ", NORMAL));

      document.add(line("Period: ", run.getYear() + "-" + run.getMonth()));
      document.add(line("Employee: ", safe(employeeName)));
      document.add(line("Employee ID: ", safe(line.getEmployeeId())));
      document.add(line("Working Days: ", String.valueOf(run.getWorkingDays())));
      document.add(line("LOP Days: ", String.valueOf(line.getLopDays())));
      document.add(new Paragraph(" ", NORMAL));

      document.add(new Paragraph("Earnings", SECTION));
      document.add(line("Gross: ", Money.ofMinor(line.getGrossMinor()).toString()));
      document.add(line("Incentives: ", Money.ofMinor(line.getIncentivesMinor()).toString()));
      document.add(line("Earned Gross: ", Money.ofMinor(line.getEarnedGrossMinor()).toString()));
      document.add(new Paragraph(" ", NORMAL));

      document.add(new Paragraph("Deductions", SECTION));
      document.add(line("PF: ", Money.ofMinor(line.getPfMinor()).toString()));
      document.add(line("ESI: ", Money.ofMinor(line.getEsiMinor()).toString()));
      document.add(line("PT: ", Money.ofMinor(line.getPtMinor()).toString()));
      document.add(line("TDS: ", Money.ofMinor(line.getTdsMinor()).toString()));
      document.add(line("Other: ", Money.ofMinor(line.getOtherDeductionsMinor()).toString()));
      document.add(new Paragraph(" ", NORMAL));

      Paragraph net =
          new Paragraph("Net Pay: " + Money.ofMinor(line.getNetPayMinor()).toString(), SECTION);
      document.add(net);

      document.close();
      return out.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException("Failed to render payslip PDF", e);
    }
  }

  private static Paragraph line(String label, String value) {
    Paragraph p = new Paragraph();
    p.add(new Phrase(label, LABEL));
    p.add(new Phrase(value, NORMAL));
    return p;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}

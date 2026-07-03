package com.paywithease.ocr.application;

import com.paywithease.ocr.domain.BankDetailExtractor;
import com.paywithease.ocr.domain.DocumentRecord;
import com.paywithease.ocr.domain.DocumentType;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentOcrProvider implements OcrProvider {

  private final BankDetailExtractor bankDetailExtractor;

  public DevelopmentOcrProvider(BankDetailExtractor bankDetailExtractor) {
    this.bankDetailExtractor = bankDetailExtractor;
  }

  @Override
  public OcrExtraction extract(
      DocumentRecord document, DocumentType documentType, String rawTextHint) {
    if (documentType == DocumentType.BANK_DETAILS
        || documentType == DocumentType.CHEQUE
        || documentType == DocumentType.PASSBOOK) {
      BankDetailExtractor.BankDetailExtraction extraction =
          bankDetailExtractor.extract(rawTextHint);
      return new OcrExtraction(extraction.fields(), extraction.confidence(), rawTextHint);
    }
    return new OcrExtraction(Map.of(), BigDecimal.ZERO, rawTextHint);
  }
}

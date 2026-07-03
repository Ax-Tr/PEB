package com.paywithease.ocr.application;

import com.paywithease.ocr.domain.BankDetailExtractor;
import com.paywithease.ocr.domain.DocumentRecord;
import com.paywithease.ocr.domain.DocumentType;
import java.math.BigDecimal;
import java.util.Map;

public interface OcrProvider {

  OcrExtraction extract(DocumentRecord document, DocumentType documentType, String rawTextHint);

  record OcrExtraction(
      Map<String, BankDetailExtractor.ExtractedField> fields,
      BigDecimal confidence,
      String rawText) {}
}

package com.paywithease.ocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.paywithease")
public class OcrDocumentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OcrDocumentServiceApplication.class, args);
  }
}

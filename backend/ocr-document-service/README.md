# OCR Document Service

Sprint 22 service for document capture and bank-detail OCR review.

The service stores document metadata, encrypted OCR text and extracted bank fields, then requires a
human review decision before the frontend can save the details into `vendor-service` as an OCR-sourced
pending bank account.

```bash
./gradlew :ocr-document-service:bootRun
```

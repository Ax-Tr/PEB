@echo off
set CUSTOMER_URI=http://localhost:8104

start "api-gateway" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\api-gateway\build\libs\api-gateway-0.1.0-SNAPSHOT.jar

set PORT=8104
start "customer-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\customer-service\build\libs\customer-service-0.1.0-SNAPSHOT.jar
set PORT=

start "identity-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\identity-service\build\libs\identity-service-0.1.0-SNAPSHOT.jar
start "vendor-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\vendor-service\build\libs\vendor-service-0.1.0-SNAPSHOT.jar
start "analytics-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\analytics-service\build\libs\analytics-service-0.1.0-SNAPSHOT.jar
start "tenant-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\tenant-service\build\libs\tenant-service-0.1.0-SNAPSHOT.jar
start "product-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\product-service\build\libs\product-service-0.1.0-SNAPSHOT.jar
start "employee-payroll-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\employee-payroll-service\build\libs\employee-payroll-service-0.1.0-SNAPSHOT.jar
start "payment-collection-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\payment-collection-service\build\libs\payment-collection-service-0.1.0-SNAPSHOT.jar
start "invoice-gst-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\invoice-gst-service\build\libs\invoice-gst-service-0.1.0-SNAPSHOT.jar
start "accounting-ledger-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\accounting-ledger-service\build\libs\accounting-ledger-service-0.1.0-SNAPSHOT.jar
start "purchase-expense-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\purchase-expense-service\build\libs\purchase-expense-service-0.1.0-SNAPSHOT.jar
start "payout-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\payout-service\build\libs\payout-service-0.1.0-SNAPSHOT.jar
start "installment-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\installment-service\build\libs\installment-service-0.1.0-SNAPSHOT.jar
start "commitment-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\commitment-service\build\libs\commitment-service-0.1.0-SNAPSHOT.jar
start "notification-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\notification-service\build\libs\notification-service-0.1.0-SNAPSHOT.jar
start "transaction-ingestion-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\transaction-ingestion-service\build\libs\transaction-ingestion-service-0.1.0-SNAPSHOT.jar
start "reconciliation-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\reconciliation-service\build\libs\reconciliation-service-0.1.0-SNAPSHOT.jar
start "compliance-report-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\compliance-report-service\build\libs\compliance-report-service-0.1.0-SNAPSHOT.jar
start "audit-evidence-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\audit-evidence-service\build\libs\audit-evidence-service-0.1.0-SNAPSHOT.jar
start "ca-collaboration-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\ca-collaboration-service\build\libs\ca-collaboration-service-0.1.0-SNAPSHOT.jar
start "ai-automation-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\ai-automation-service\build\libs\ai-automation-service-0.1.0-SNAPSHOT.jar
start "privacy-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\privacy-service\build\libs\privacy-service-0.1.0-SNAPSHOT.jar
start "ocr-document-service" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\ocr-document-service\build\libs\ocr-document-service-0.1.0-SNAPSHOT.jar
start "common-libraries" java -Xmx256m -Dspring.profiles.active=local -jar C:\Users\"Axiora User-36"\Desktop\"New folder"\PEB\backend\common-libraries\build\libs\common-libraries-0.1.0-SNAPSHOT.jar

echo Started 25 services!

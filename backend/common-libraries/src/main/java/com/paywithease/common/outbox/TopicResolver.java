package com.paywithease.common.outbox;

/**
 * Maps an event type to its Kafka topic (the topic list in docs/architecture-blueprint.md §5).
 * Routing is by event family; order matters because some families share a prefix (e.g. {@code
 * VENDOR_PAYMENT_*} is a payout event, whereas {@code VENDOR_CREATED} is a master-data event).
 * Topics here must line up with what each service's {@code @KafkaListener} subscribes to. Services
 * may provide their own bean to override.
 */
@FunctionalInterface
public interface TopicResolver {

  String topicFor(String eventType);

  static TopicResolver defaultResolver() {
    return eventType -> {
      if (eventType == null) {
        return "peb.misc.events";
      }
      // Payout family — MUST be checked before the generic VENDOR_/master-data rules below.
      if (eventType.startsWith("VENDOR_PAYMENT_") || eventType.startsWith("PAYOUT_")) {
        return "payout.events";
      }
      if (eventType.startsWith("PAYMENT_")) return "payment.events";
      if (eventType.startsWith("INVOICE_")) return "invoice.events";
      if (eventType.startsWith("PURCHASE_") || eventType.startsWith("EXPENSE_")) {
        return "purchase.events";
      }
      if (eventType.startsWith("SALARY_") || eventType.startsWith("PAYSLIP_")) {
        return "payroll.events";
      }
      if (eventType.startsWith("BANK_TRANSACTION_") || eventType.startsWith("TRANSACTION_")) {
        return "ingestion.events";
      }
      if (eventType.startsWith("RECONCILIATION_")) return "reconciliation.events";
      if (eventType.startsWith("REMINDER_") || eventType.startsWith("NOTIFICATION_")) {
        return "notification.events";
      }
      if (eventType.startsWith("INSTALLMENT_")) return "installment.events";
      if (eventType.startsWith("JOURNAL_") || eventType.startsWith("MONTH_"))
        return "ledger.events";
      if (eventType.startsWith("BUSINESS_")) return "tenant.events";
      if (eventType.startsWith("USER_") || eventType.startsWith("DEVICE_"))
        return "identity.events";
      // Master data (customer/vendor/employee/product creation & updates).
      if (eventType.startsWith("CUSTOMER_")
          || eventType.startsWith("VENDOR_")
          || eventType.startsWith("EMPLOYEE_")
          || eventType.startsWith("PRODUCT_")) {
        return "masters.events";
      }
      return "peb.misc.events";
    };
  }
}

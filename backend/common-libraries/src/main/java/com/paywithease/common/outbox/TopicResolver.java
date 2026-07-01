package com.paywithease.common.outbox;

/**
 * Maps an event type to its Kafka topic. The default groups events by domain prefix (matching the
 * topic list in docs/architecture-blueprint.md §5). Services may provide their own bean to
 * override.
 */
@FunctionalInterface
public interface TopicResolver {

  String topicFor(String eventType);

  static TopicResolver defaultResolver() {
    return eventType -> {
      if (eventType == null) {
        return "peb.misc.events";
      }
      if (eventType.startsWith("PAYMENT_")) return "payment.events";
      if (eventType.startsWith("INVOICE_")) return "invoice.events";
      if (eventType.startsWith("BUSINESS_")) return "tenant.events";
      if (eventType.startsWith("USER_") || eventType.startsWith("DEVICE_"))
        return "identity.events";
      if (eventType.startsWith("CUSTOMER_")) return "masters.events";
      if (eventType.startsWith("VENDOR_")) return "masters.events";
      if (eventType.startsWith("EMPLOYEE_")) return "masters.events";
      if (eventType.startsWith("PRODUCT_")) return "masters.events";
      if (eventType.startsWith("JOURNAL_") || eventType.startsWith("MONTH_"))
        return "ledger.events";
      if (eventType.startsWith("INSTALLMENT_")) return "installment.events";
      return "peb.misc.events";
    };
  }
}

package com.paywithease.purchase.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.gst.GstCalculator;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.money.Money;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.purchase.domain.Expense;
import com.paywithease.purchase.domain.PurchaseBill;
import com.paywithease.purchase.domain.PurchaseItem;
import com.paywithease.purchase.infrastructure.ExpenseRepository;
import com.paywithease.purchase.infrastructure.PurchaseBillRepository;
import com.paywithease.purchase.infrastructure.PurchaseItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purchase &amp; expense recording: computes input GST (ITC) via {@link GstCalculator}, persists
 * the purchase bill with its items, records expenses through maker-checker approval, and emits
 * PURCHASE_BILL_CREATED / EXPENSE_APPROVED events the ledger consumes. All amounts are integer
 * paise.
 */
@Service
public class PurchaseService {

  private static final String SOURCE = "purchase-expense-service";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final PurchaseBillRepository bills;
  private final PurchaseItemRepository items;
  private final ExpenseRepository expenses;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final String defaultStateCode;

  public PurchaseService(
      PurchaseBillRepository bills,
      PurchaseItemRepository items,
      ExpenseRepository expenses,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.purchase.default-state-code:27}") String defaultStateCode) {
    this.bills = bills;
    this.items = items;
    this.expenses = expenses;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.defaultStateCode = defaultStateCode;
  }

  public record LineCommand(
      String productId,
      String description,
      String hsnSac,
      BigDecimal quantity,
      long unitPriceMinor,
      long discountMinor,
      BigDecimal gstRate) {}

  public record CreateBillCommand(
      String vendorId,
      String vendorName,
      String vendorGstin,
      String billNumber,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      LocalDate billDate,
      List<LineCommand> lines) {}

  @Transactional
  public PurchaseBill createBill(CreateBillCommand cmd) {
    String tenantId = TenantContext.requireTenantId();

    if (cmd.lines() == null || cmd.lines().isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "at least one line is required");
    }

    String businessState =
        isBlank(cmd.businessStateCode()) ? defaultStateCode : cmd.businessStateCode();
    String placeOfSupply = resolvePlaceOfSupply(cmd, businessState);
    LocalDate date =
        cmd.billDate() != null ? cmd.billDate() : LocalDate.ofInstant(clock.instant(), IST);

    // Compute per-line taxable value (paise) and gather GST inputs.
    List<Long> taxableValues = new ArrayList<>();
    List<GstCalculator.LineInput> inputs = new ArrayList<>();
    for (LineCommand line : cmd.lines()) {
      long taxableValue = taxableValueMinor(line);
      taxableValues.add(taxableValue);
      BigDecimal rate = line.gstRate() == null ? BigDecimal.ZERO : line.gstRate();
      inputs.add(new GstCalculator.LineInput(taxableValue, rate));
    }

    // A purchase always computes input GST → taxable = true.
    GstCalculator.Result result =
        GstCalculator.compute(businessState, placeOfSupply, inputs, true, cmd.reverseCharge());

    Instant now = clock.instant();
    String billId = Ulid.newId();
    PurchaseBill bill =
        new PurchaseBill(
            billId,
            tenantId,
            cmd.vendorId(),
            cmd.vendorName(),
            cmd.vendorGstin(),
            cmd.billNumber(),
            date,
            placeOfSupply,
            businessState,
            cmd.reverseCharge(),
            result.totalTaxableMinor(),
            result.totalTaxMinor(),
            result.totalCgstMinor(),
            result.totalSgstMinor(),
            result.totalIgstMinor(),
            result.documentTotalMinor(),
            now);
    bills.save(bill);

    for (int i = 0; i < cmd.lines().size(); i++) {
      LineCommand line = cmd.lines().get(i);
      long taxableValue = taxableValues.get(i);
      GstCalculator.LineTax lineTax = result.lines().get(i);
      long lineTotal = cmd.reverseCharge() ? taxableValue : taxableValue + lineTax.totalTaxMinor();
      items.save(
          new PurchaseItem(
              Ulid.newId(),
              tenantId,
              billId,
              line.productId(),
              line.description(),
              line.hsnSac(),
              line.quantity(),
              line.unitPriceMinor(),
              line.discountMinor(),
              line.gstRate() == null ? BigDecimal.ZERO : line.gstRate(),
              taxableValue,
              lineTax.cgstMinor(),
              lineTax.sgstMinor(),
              lineTax.igstMinor(),
              lineTotal));
    }

    audit.record(
        "PURCHASE_BILL_CREATED",
        "purchase_bill",
        billId,
        Map.of(
            "vendorId", nullSafe(cmd.vendorId()),
            "billNumber", nullSafe(cmd.billNumber()),
            "totalAmountMinor", bill.getTotalAmountMinor(),
            "totalInputGstMinor", bill.getTotalInputGstMinor()));

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("purchaseBillId", billId);
    payload.put("vendorId", cmd.vendorId());
    payload.put("netMinor", bill.getTotalTaxableMinor());
    payload.put("inputGstMinor", bill.getTotalInputGstMinor());
    payload.put("totalAmountMinor", bill.getTotalAmountMinor());
    payload.put("reverseCharge", bill.isReverseCharge());
    emit("PURCHASE_BILL_CREATED", bill.getTenantId(), billId, payload);

    return bill;
  }

  @Transactional(readOnly = true)
  public PurchaseBill getBill(String id) {
    return bills
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Purchase bill"));
  }

  @Transactional(readOnly = true)
  public List<PurchaseItem> items(String billId) {
    return items.findByPurchaseBillId(billId);
  }

  @Transactional(readOnly = true)
  public List<PurchaseBill> register(LocalDate from, LocalDate to) {
    return bills.findByTenantIdAndBillDateBetweenOrderByBillDateAsc(
        TenantContext.requireTenantId(), from, to);
  }

  @Transactional
  public Expense createExpense(
      String category,
      String description,
      long amountMinor,
      BigDecimal gstRate,
      String vendorId,
      LocalDate expenseDate) {
    String tenantId = TenantContext.requireTenantId();
    BigDecimal rate = gstRate == null ? BigDecimal.ZERO : gstRate;
    long inputGst = Money.ofMinor(amountMinor).percent(rate).toMinor();
    LocalDate date = expenseDate != null ? expenseDate : LocalDate.ofInstant(clock.instant(), IST);

    Instant now = clock.instant();
    String expenseId = Ulid.newId();
    Expense expense =
        new Expense(
            expenseId,
            tenantId,
            category,
            description,
            amountMinor,
            rate,
            inputGst,
            vendorId,
            date,
            now);
    expenses.save(expense);

    audit.record(
        "EXPENSE_CREATED",
        "expense",
        expenseId,
        Map.of(
            "category", nullSafe(category),
            "amountMinor", amountMinor,
            "inputGstMinor", inputGst));

    return expense;
  }

  @Transactional
  public Expense approveExpense(String id, String approverId) {
    String tenantId = TenantContext.requireTenantId();
    Expense expense =
        expenses
            .findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> ApiException.notFound("Expense"));
    if (!expense.isPendingApproval()) {
      throw new ApiException(
          ErrorCode.CONFLICT,
          "Expense is not pending approval (status " + expense.getStatus() + ")");
    }
    expense.approve(approverId, clock.instant());
    expenses.save(expense);

    audit.record(
        "EXPENSE_APPROVED",
        "expense",
        id,
        Map.of("approvedBy", nullSafe(approverId), "amountMinor", expense.getAmountMinor()));

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("expenseId", id);
    payload.put("amountMinor", expense.getAmountMinor());
    payload.put("category", expense.getCategory());
    emit("EXPENSE_APPROVED", expense.getTenantId(), id, payload);

    return expense;
  }

  @Transactional(readOnly = true)
  public List<Expense> listExpenses() {
    return expenses.findByTenantIdOrderByExpenseDateDesc(TenantContext.requireTenantId());
  }

  private String resolvePlaceOfSupply(CreateBillCommand cmd, String businessState) {
    if (!isBlank(cmd.placeOfSupply())) {
      return cmd.placeOfSupply();
    }
    if (cmd.vendorGstin() != null && cmd.vendorGstin().length() >= 2) {
      return cmd.vendorGstin().substring(0, 2);
    }
    return businessState;
  }

  private long taxableValueMinor(LineCommand line) {
    BigDecimal quantity = line.quantity() == null ? BigDecimal.ONE : line.quantity();
    long gross =
        quantity
            .multiply(BigDecimal.valueOf(line.unitPriceMinor()))
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
    long net = gross - line.discountMinor();
    if (net < 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "discount exceeds line value");
    }
    return net;
  }

  private void emit(String eventType, String tenantId, String aggregateId, ObjectNode payload) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }
}

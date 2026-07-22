/**
 * Centralized mock responses for demo mode. Every object precisely matches the backend Java record
 * DTOs (field names, types, nesting). The frontend {@link types.ts} already mirrors these; mock
 * values here are realistic Indian-business samples so the UI looks production-like.
 *
 * Source of truth:
 *   - AnalyticsDtos.java      → PnlResponse, AgingResponse, CashflowResponse, etc.
 *   - AuthDtos.java           → AuthResponse (OtpRequestResult, TokenPair on frontend)
 *   - PaymentDtos.java        → PaymentResponse
 *   - PayoutDtos.java         → PayoutResponse, CreatePayoutResponse
 *   - InvoiceDtos.java        → InvoiceResponse (with items + taxLines)
 *   - ReconciliationDtos.java → MatchResponse, RunResponse
 *   - ComplianceDtos.java     → ReportResponse, ReportLineResponse
 *   - PrivacyDtos.java        → DsrResponse, ErasurePlanResponse
 *   - AiDtos.java             → SuggestionResponse, AnomalyAlertResponse, AssistantResponse, VoiceDraftResponse
 *   - CustomerDtos.java       → CustomerResponse
 *   - VendorDtos.java         → VendorList (wrapping VendorResponse[]), BankAccountList
 *   - CommitmentDtos.java     → CommitmentResponse, CommitmentDetailResponse, CommitmentEventResponse
 *   - InstallmentDtos.java    → InstallmentResponse (with EmiResponse[])
 *   - NotificationDtos.java   → ReminderResponse, CountResponse
 *   - OcrDtos.java            → OcrJobResponse, UploadReservationResponse
 *   - BusinessDtos.java       → BusinessResponse
 */

// ---------------------------------------------------------------------------
// Analytics — matches AnalyticsDtos.java records
// ---------------------------------------------------------------------------

/** Matches AnalyticsDtos.PnlResponse (all amounts in paise / minor). */
export const MOCK_PNL = {
  revenueMinor: 1_45_000_00,       // ₹1,45,000
  directCostMinor: 62_000_00,      // ₹62,000
  operatingExpenseMinor: 28_500_00, // ₹28,500
  grossProfitMinor: 83_000_00,     // ₹83,000
  netProfitMinor: 54_500_00,       // ₹54,500
  grossMarginPct: "57.24",
  netMarginPct: "37.59",
};

/** Matches AnalyticsDtos.AgingResponse with nested AgingBucketResponse[]. */
export const MOCK_RECEIVABLES_AGING = {
  buckets: [
    { bucket: "DAYS_0_30", totalMinor: 42_000_00, count: 5 },
    { bucket: "DAYS_31_60", totalMinor: 18_500_00, count: 3 },
    { bucket: "DAYS_61_90", totalMinor: 7_200_00, count: 2 },
    { bucket: "DAYS_90_PLUS", totalMinor: 3_800_00, count: 1 },
  ],
  totalOutstandingMinor: 71_500_00,
  totalCount: 11,
};

export const MOCK_PAYABLES_AGING = {
  buckets: [
    { bucket: "DAYS_0_30", totalMinor: 35_000_00, count: 4 },
    { bucket: "DAYS_31_60", totalMinor: 12_000_00, count: 2 },
    { bucket: "DAYS_61_90", totalMinor: 5_000_00, count: 1 },
    { bucket: "DAYS_90_PLUS", totalMinor: 0, count: 0 },
  ],
  totalOutstandingMinor: 52_000_00,
  totalCount: 7,
};

/** Matches AnalyticsDtos.CashflowResponse with nested CashflowPeriodResponse[]. */
export const MOCK_CASHFLOW = {
  openingBalanceMinor: 2_50_000_00,
  totalInflowMinor: 4_20_000_00,
  totalOutflowMinor: 3_15_000_00,
  netMinor: 1_05_000_00,
  closingBalanceMinor: 3_55_000_00,
  periods: [
    { year: 2026, month: 4, inflowMinor: 1_10_000_00, outflowMinor: 95_000_00, netMinor: 15_000_00, closingBalanceMinor: 2_65_000_00 },
    { year: 2026, month: 5, inflowMinor: 1_35_000_00, outflowMinor: 1_05_000_00, netMinor: 30_000_00, closingBalanceMinor: 2_95_000_00 },
    { year: 2026, month: 6, inflowMinor: 1_75_000_00, outflowMinor: 1_15_000_00, netMinor: 60_000_00, closingBalanceMinor: 3_55_000_00 },
  ],
};

/** Matches List<AnalyticsDtos.ProductProfitabilityResponse>. */
export const MOCK_PRODUCT_PROFITABILITY = [
  { productId: "01J5KM2R3N4P5Q6R7S8T9U0V", productName: "IT Consulting (per hour)", revenueMinor: 60_000_00, costMinor: 18_000_00, profitMinor: 42_000_00, marginPct: "70.00" },
  { productId: "01J5KM3A4B5C6D7E8F9G0H1I", productName: "Cloud Hosting (monthly)", revenueMinor: 45_000_00, costMinor: 22_500_00, profitMinor: 22_500_00, marginPct: "50.00" },
  { productId: "01J5KM4J5K6L7M8N9O0P1Q2R", productName: "SEO Package", revenueMinor: 25_000_00, costMinor: 12_000_00, profitMinor: 13_000_00, marginPct: "52.00" },
];

/** Matches AnalyticsDtos.CommitmentSummaryResponse. */
export const MOCK_COMMITMENT_SUMMARY = {
  openCount: 8,
  dueTodayCount: 2,
  overdueCount: 1,
  brokenCount: 0,
  openOutstandingMinor: 1_20_000_00,
  dueTodayMinor: 35_000_00,
  overdueMinor: 15_000_00,
  dueSoonMinor: 50_000_00,
};

/** Matches AnalyticsDtos.CollectionEfficiencyResponse. */
export const MOCK_COLLECTION_EFFICIENCY = {
  promisedMinor: 2_00_000_00,
  collectedMinor: 1_72_000_00,
  conversionPct: "86.00",
};

/** Matches List<AnalyticsDtos.CommitmentItemResponse>. */
export const MOCK_BROKEN_PROMISES: Array<{
  commitmentId: string;
  counterpartyType: string;
  counterpartyId: string | null;
  counterpartyName: string | null;
  dueDate: string;
  outstandingMinor: number;
  status: string;
}> = [
  { commitmentId: "01J5BR0K1A2B3C4D5E6F7G8H", counterpartyType: "CUSTOMER", counterpartyId: "01J5CU1A2B3C4D5E6F7G8H9I", counterpartyName: "Rajesh Electronics", dueDate: "2026-07-10", outstandingMinor: 15_000_00, status: "BROKEN" },
];

/** Matches List<AnalyticsDtos.CommitmentItemResponse>. */
export const MOCK_UPCOMING_OBLIGATIONS = [
  { commitmentId: "01J5UP0K1A2B3C4D5E6F7G8H", counterpartyType: "VENDOR", counterpartyId: "01J5VE1A2B3C4D5E6F7G8H9I", counterpartyName: "Sharma Packaging", dueDate: "2026-07-25", outstandingMinor: 25_000_00, status: "OPEN" },
  { commitmentId: "01J5UP1L2B3C4D5E6F7G8H9I", counterpartyType: "CUSTOMER", counterpartyId: "01J5CU2B3C4D5E6F7G8H9I0J", counterpartyName: "Priya Textiles", dueDate: "2026-07-28", outstandingMinor: 18_000_00, status: "OPEN" },
];

/** Matches List<AnalyticsDtos.FreshnessResponse>. lastProcessedAt is Instant (ISO-8601). */
export const MOCK_FRESHNESS = [
  { stream: "invoices", state: "FRESH", lagSeconds: 12, lastProcessedAt: "2026-07-22T09:00:00Z" },
  { stream: "payments", state: "FRESH", lagSeconds: 8, lastProcessedAt: "2026-07-22T09:00:05Z" },
  { stream: "expenses", state: "FRESH", lagSeconds: 45, lastProcessedAt: "2026-07-22T08:59:30Z" },
];

// ---------------------------------------------------------------------------
// Invoices — matches InvoiceDtos.InvoiceResponse (with items + taxLines)
// ---------------------------------------------------------------------------

export const MOCK_INVOICES = [
  {
    id: "01J5IN1A2B3C4D5E6F7G8H9I",
    documentType: "TAX_INVOICE",
    supplyType: "B2B",
    invoiceNumber: "INV/2026-27/001",
    financialYear: "2026-27",
    invoiceDate: "2026-07-15",
    customerName: "Priya Textiles Pvt Ltd",
    customerGstin: "27AABCP1234C1ZV",
    placeOfSupply: "27",
    reverseCharge: false,
    taxable: true,
    totalTaxableMinor: 50_000_00,
    totalCgstMinor: 4_500_00,
    totalSgstMinor: 4_500_00,
    totalIgstMinor: 0,
    totalTaxMinor: 9_000_00,
    totalAmountMinor: 59_000_00,
    status: "SENT",
    items: [
      { productId: "01J5KM2R3N4P5Q6R7S8T9U0V", description: "IT Consulting", hsnSac: "998314", quantity: 10, unitPriceMinor: 5_000_00, discountMinor: 0, gstRate: 18, taxableValueMinor: 50_000_00, cgstMinor: 4_500_00, sgstMinor: 4_500_00, igstMinor: 0, lineTotalMinor: 59_000_00 },
    ],
    taxLines: [
      { gstRate: 18, taxableValueMinor: 50_000_00, cgstMinor: 4_500_00, sgstMinor: 4_500_00, igstMinor: 0 },
    ],
  },
  {
    id: "01J5IN2B3C4D5E6F7G8H9I0J",
    documentType: "TAX_INVOICE",
    supplyType: "B2C",
    invoiceNumber: "INV/2026-27/002",
    financialYear: "2026-27",
    invoiceDate: "2026-07-18",
    customerName: "Walk-in Customer",
    customerGstin: null,
    placeOfSupply: "27",
    reverseCharge: false,
    taxable: true,
    totalTaxableMinor: 12_000_00,
    totalCgstMinor: 1_080_00,
    totalSgstMinor: 1_080_00,
    totalIgstMinor: 0,
    totalTaxMinor: 2_160_00,
    totalAmountMinor: 14_160_00,
    status: "CREATED",
    items: [
      { productId: "01J5KM4J5K6L7M8N9O0P1Q2R", description: "SEO Package (Basic)", hsnSac: "998361", quantity: 1, unitPriceMinor: 12_000_00, discountMinor: 0, gstRate: 18, taxableValueMinor: 12_000_00, cgstMinor: 1_080_00, sgstMinor: 1_080_00, igstMinor: 0, lineTotalMinor: 14_160_00 },
    ],
    taxLines: [
      { gstRate: 18, taxableValueMinor: 12_000_00, cgstMinor: 1_080_00, sgstMinor: 1_080_00, igstMinor: 0 },
    ],
  },
];

// ---------------------------------------------------------------------------
// Payouts — matches PayoutDtos.PayoutResponse
// ---------------------------------------------------------------------------

export const MOCK_PAYOUTS = [
  { id: "01J5PO1A2B3C4D5E6F7G8H9I", partyType: "VENDOR", partyId: "01J5VE1A2B3C4D5E6F7G8H9I", beneficiaryId: "01J5BE1A2B3C4D5E6F7G8H9I", amountMinor: 45_000_00, status: "COMPLETED", riskLevel: "LOW", provider: "RAZORPAY", providerRef: "pout_Abc123XyZ" },
  { id: "01J5PO2B3C4D5E6F7G8H9I0J", partyType: "VENDOR", partyId: "01J5VE2B3C4D5E6F7G8H9I0J", beneficiaryId: "01J5BE2B3C4D5E6F7G8H9I0J", amountMinor: 78_500_00, status: "PENDING_APPROVAL", riskLevel: "HIGH", provider: null, providerRef: null },
  { id: "01J5PO3C4D5E6F7G8H9I0J1K", partyType: "EMPLOYEE", partyId: "01J5EM1A2B3C4D5E6F7G8H9I", beneficiaryId: "01J5BE3C4D5E6F7G8H9I0J1K", amountMinor: 35_000_00, status: "COMPLETED", riskLevel: "LOW", provider: "RAZORPAY", providerRef: "pout_Def456UvW" },
];

// ---------------------------------------------------------------------------
// Payment requests — matches PaymentDtos.PaymentResponse
// Note: backend returns requestId (not paymentRequestId), reference, amountMinor,
//       amountPaidMinor, status, upiUri, paymentLink
// The frontend types.ts says paymentRequestId + reference + status + amountMinor + upiUri.
// We must match what the backend *actually* returns.
// ---------------------------------------------------------------------------

export const MOCK_PAYMENT_REQUEST = {
  requestId: "01J5PR1A2B3C4D5E6F7G8H9I",
  reference: "PEB-PR-20260722-001",
  amountMinor: 10_000_00,
  amountPaidMinor: 0,
  status: "CREATED",
  upiUri: "upi://pay?pa=demo@peb&pn=PayWithEase&am=10000.00&cu=INR&tn=PEB-PR-20260722-001",
  paymentLink: "https://pay.paywithease.in/pr/01J5PR1A2B3C4D5E6F7G8H9I",
};

// ---------------------------------------------------------------------------
// Reconciliation — matches ReconciliationDtos.MatchResponse, RunResponse
// ---------------------------------------------------------------------------

export const MOCK_RECON_SUGGESTIONS = [
  { id: "01J5RC1A2B3C4D5E6F7G8H9I", externalItemId: "01J5EX1A2B3C4D5E6F7G8H9I", internalItemId: "01J5IX1A2B3C4D5E6F7G8H9I", status: "SUGGESTED" },
  { id: "01J5RC2B3C4D5E6F7G8H9I0J", externalItemId: "01J5EX2B3C4D5E6F7G8H9I0J", internalItemId: "01J5IX2B3C4D5E6F7G8H9I0J", status: "SUGGESTED" },
];

export const MOCK_RECON_RUN = { autoMatched: 12, suggested: 3, exceptionsCreated: 1 };

// ---------------------------------------------------------------------------
// Compliance — matches ComplianceDtos.ReportResponse, ReportLineResponse
// ---------------------------------------------------------------------------

export const MOCK_COMPLIANCE_REPORTS = [
  {
    id: "01J5CR1A2B3C4D5E6F7G8H9I",
    type: "GSTR3B_SUMMARY",
    year: 2026,
    month: 6,
    status: "DRAFT",
    displayState: "DRAFT",
    dataReconciled: false,
    totalTaxableMinor: 1_45_000_00,
    totalTaxMinor: 26_100_00,
    netPayableMinor: 26_100_00,
    missingFields: [] as string[],
    ackReference: null as string | null,
  },
  {
    id: "01J5CR2B3C4D5E6F7G8H9I0J",
    type: "GSTR1_SUMMARY",
    year: 2026,
    month: 5,
    status: "FILED",
    displayState: "FILED",
    dataReconciled: true,
    totalTaxableMinor: 1_30_000_00,
    totalTaxMinor: 23_400_00,
    netPayableMinor: 23_400_00,
    missingFields: [] as string[],
    ackReference: "ACK-GSTR1-202605-00456",
  },
];

export const MOCK_COMPLIANCE_REPORT_LINES = [
  { id: "01J5CL1A2B3C4D5E6F7G8H9I", label: "Outward supplies (taxable)", taxableMinor: 1_00_000_00, taxMinor: 18_000_00, amountMinor: 1_18_000_00 },
  { id: "01J5CL2B3C4D5E6F7G8H9I0J", label: "Outward supplies (exempt)", taxableMinor: 45_000_00, taxMinor: 0, amountMinor: 45_000_00 },
  { id: "01J5CL3C4D5E6F7G8H9I0J1K", label: "Input tax credit (ITC)", taxableMinor: 0, taxMinor: 8_100_00, amountMinor: 8_100_00 },
];

// ---------------------------------------------------------------------------
// Privacy / DPDP — matches PrivacyDtos.DsrResponse, ErasurePlanResponse
// ---------------------------------------------------------------------------

export const MOCK_DSR_REQUESTS = [
  {
    id: "01J5DS1A2B3C4D5E6F7G8H9I",
    type: "ACCESS",
    status: "IN_PROGRESS",
    subjectRef: "USR-10045",
    subjectEmail: "customer@example.com",
    details: "Request for all stored personal data under DPDP Section 11",
    erasurePlan: null,
    resolutionNote: null,
    evidenceRef: null,
    dueAt: "2026-08-10T18:30:00Z",
  },
  {
    id: "01J5DS2B3C4D5E6F7G8H9I0J",
    type: "ERASURE",
    status: "RECEIVED",
    subjectRef: "USR-10072",
    subjectEmail: "vendor@example.com",
    details: "Erase all marketing and contact data",
    erasurePlan: null,
    resolutionNote: null,
    evidenceRef: null,
    dueAt: "2026-08-15T18:30:00Z",
  },
];

export const MOCK_ERASURE_PLAN = {
  lines: [
    { category: "PROFILE_PII", action: "DELETE", minRetentionYears: 0, reason: "No legal hold; eligible for immediate erasure" },
    { category: "CONTACT_PII", action: "DELETE", minRetentionYears: 0, reason: "No active contract" },
    { category: "FINANCIAL_TXN", action: "RETAIN_LEGAL_HOLD", minRetentionYears: 8, reason: "Income Tax Act §149 requires 8-year retention of financial records" },
    { category: "TAX_RECORD", action: "RETAIN_LEGAL_HOLD", minRetentionYears: 8, reason: "GST Act §36 requires 8-year retention" },
    { category: "KYC_PII", action: "ANONYMIZE", minRetentionYears: 5, reason: "PMLA §12 KYC records retained 5 years post relationship" },
  ],
  fullErasurePossible: false,
  summary: "Financial/tax records retained per Indian law; profile & contact PII erased, KYC anonymised.",
};

// ---------------------------------------------------------------------------
// AI — matches AiDtos.SuggestionResponse, AnomalyAlertResponse,
//       AssistantResponse, VoiceDraftResponse
// ---------------------------------------------------------------------------

export const MOCK_AI_SUGGESTIONS = [
  {
    id: "01J5AS1A2B3C4D5E6F7G8H9I",
    kind: "TXN_CATEGORY",
    subjectType: "BANK_TRANSACTION",
    subjectId: "01J5TX1A2B3C4D5E6F7G8H9I",
    suggestion: { category: "OFFICE_SUPPLIES", narration: "AMAZON BUSINESS PURCHASE" },
    confidence: "0.92",
    decision: "NEEDS_REVIEW",
    status: "PROPOSED",
    modelRef: "category-clf-v3",
  },
  {
    id: "01J5AS2B3C4D5E6F7G8H9I0J",
    kind: "CASHFLOW_FORECAST",
    subjectType: "TENANT",
    subjectId: null,
    suggestion: { forecastNetMinor: 85_000_00, horizon: "next_month" },
    confidence: "0.78",
    decision: "NEEDS_REVIEW",
    status: "PROPOSED",
    modelRef: "forecast-ema-v1",
  },
];

export const MOCK_AI_ANOMALIES = [
  {
    id: "01J5AN1A2B3C4D5E6F7G8H9I",
    subjectType: "EXPENSE",
    subjectId: "01J5EXP1A2B3C4D5E6F7G8H",
    observedMinor: 2_50_000_00,
    score: "0.95",
    severity: "HIGH",
    status: "OPEN",
    detail: "Expense ₹2,50,000 is 4.2σ above the 90-day mean for this category",
  },
];

export const MOCK_ASSISTANT_ANSWER = {
  answer: "Your net profit margin this month is 37.6%, which is 5.2pp above the trailing 3-month average. The main driver is higher consulting revenue (+₹20,000) with stable costs.",
  confidence: 0.85,
  modelAvailable: true,
  injectionDetected: false,
};

export const MOCK_VOICE_DRAFTS = [
  {
    id: "01J5VD1A2B3C4D5E6F7G8H9I",
    transcript: "Create an invoice for Priya Textiles twenty five thousand rupees for web development",
    sanitizedTranscript: "Create an invoice for Priya Textiles 25000 rupees for web development",
    intent: "CREATE_INVOICE",
    status: "NEEDS_REVIEW",
    fields: { customerName: "Priya Textiles", amountMinor: 25_000_00, description: "Web development" },
    missingFields: ["hsnSac", "gstRate"],
    confidence: "0.82",
    suspicious: false,
    materializedRef: null,
    rejectionReason: null,
    createdAt: "2026-07-22T08:30:00Z",
    updatedAt: "2026-07-22T08:30:00Z",
    reviewedAt: null,
    reviewedBy: null,
  },
];

// ---------------------------------------------------------------------------
// Masters — matches CustomerDtos.CustomerResponse, VendorDtos.VendorList,
//           VendorDtos.BankAccountList
// ---------------------------------------------------------------------------

/** GET /customers returns List<CustomerResponse> (no wrapper). */
export const MOCK_CUSTOMERS = [
  { id: "01J5CU1A2B3C4D5E6F7G8H9I", name: "Priya Textiles Pvt Ltd", mobile: "9876543210", email: "accounts@priyatextiles.in", address: "42 MG Road, Pune 411001", gstin: "27AABCP1234C1ZV", createdAt: "2026-06-01T10:00:00Z" },
  { id: "01J5CU2B3C4D5E6F7G8H9I0J", name: "Rajesh Electronics", mobile: "9898989898", email: "rajesh@relec.in", address: "15 Laxmi Nagar, Delhi 110092", gstin: "07AABCR5678D1Z3", createdAt: "2026-06-10T14:30:00Z" },
  { id: "01J5CU3C4D5E6F7G8H9I0J1K", name: "Ananya Crafts", mobile: "9123456789", email: null, address: "Shop 8, Crawford Market, Mumbai 400001", gstin: null, createdAt: "2026-07-05T09:15:00Z" },
];

/** GET /vendors returns VendorList { vendors: VendorResponse[] }. */
export const MOCK_VENDOR_LIST = {
  vendors: [
    { id: "01J5VE1A2B3C4D5E6F7G8H9I", name: "Sharma Packaging", mobile: "9876501234", email: "ops@sharmapack.in", gstin: "27AABCS9012E1Z5", address: "Plot 12, MIDC Bhosari, Pune 411026", status: "ACTIVE" },
    { id: "01J5VE2B3C4D5E6F7G8H9I0J", name: "Gupta Office Supplies", mobile: "9988776655", email: "sales@guptaoffice.in", gstin: "07AABCG3456F1Z7", address: "23 Nehru Place, Delhi 110019", status: "ACTIVE" },
  ],
};

/** GET /vendors/:id/bank-accounts returns BankAccountList { bankAccounts: BankAccountResponse[] }. */
export const MOCK_BANK_ACCOUNTS = {
  bankAccounts: [
    { id: "01J5BA1A2B3C4D5E6F7G8H9I", vendorId: "01J5VE1A2B3C4D5E6F7G8H9I", accountNumberMasked: "******1234", ifsc: "HDFC0001234", upi: "sharma@upi", bankName: "HDFC Bank", holderName: "Sharma Packaging Pvt Ltd", status: "VERIFIED", source: "MANUAL", reviewedBy: "01J5US1A2B3C4D5E6F7G8H9I" },
  ],
};

// ---------------------------------------------------------------------------
// Commitments — matches CommitmentDtos.CommitmentResponse,
//               CommitmentDetailResponse, CommitmentEventResponse
// ---------------------------------------------------------------------------

export const MOCK_COMMITMENTS = [
  {
    id: "01J5CM1A2B3C4D5E6F7G8H9I",
    counterpartyType: "CUSTOMER",
    counterpartyId: "01J5CU1A2B3C4D5E6F7G8H9I",
    counterpartyName: "Priya Textiles Pvt Ltd",
    sourceType: "INVOICE",
    sourceRef: "INV/2026-27/001",
    description: "Payment against invoice INV/2026-27/001",
    amountMinor: 59_000_00,
    paidMinor: 30_000_00,
    outstandingMinor: 29_000_00,
    dueDate: "2026-07-30",
    status: "OPEN",
    createdAt: "2026-07-15T10:30:00Z",
    updatedAt: "2026-07-20T14:00:00Z",
    closedAt: null as string | null,
  },
  {
    id: "01J5CM2B3C4D5E6F7G8H9I0J",
    counterpartyType: "VENDOR",
    counterpartyId: "01J5VE1A2B3C4D5E6F7G8H9I",
    counterpartyName: "Sharma Packaging",
    sourceType: "PURCHASE_ORDER",
    sourceRef: "PO-2026-042",
    description: "Raw material payment",
    amountMinor: 25_000_00,
    paidMinor: 0,
    outstandingMinor: 25_000_00,
    dueDate: "2026-07-25",
    status: "OPEN",
    createdAt: "2026-07-10T09:00:00Z",
    updatedAt: "2026-07-10T09:00:00Z",
    closedAt: null as string | null,
  },
];

export const MOCK_COMMITMENT_DETAIL = {
  commitment: MOCK_COMMITMENTS[0],
  events: [
    { id: "01J5CE1A2B3C4D5E6F7G8H9I", eventType: "CREATED", oldDueDate: null as string | null, newDueDate: null as string | null, amountMinor: 59_000_00, note: null as string | null, occurredAt: "2026-07-15T10:30:00Z" },
    { id: "01J5CE2B3C4D5E6F7G8H9I0J", eventType: "PAYMENT_RECORDED", oldDueDate: null as string | null, newDueDate: null as string | null, amountMinor: 30_000_00, note: "Partial payment received via NEFT", occurredAt: "2026-07-20T14:00:00Z" },
  ],
};

// ---------------------------------------------------------------------------
// Installments — matches InstallmentDtos.InstallmentResponse with EmiResponse[]
// ---------------------------------------------------------------------------

export const MOCK_INSTALLMENTS = [
  {
    id: "01J5IS1A2B3C4D5E6F7G8H9I",
    type: "RECEIVABLE" as const,
    counterpartyId: "01J5CU2B3C4D5E6F7G8H9I0J",
    counterpartyName: "Rajesh Electronics",
    sourceType: "INVOICE",
    sourceRef: "INV/2026-27/003",
    totalAmountMinor: 60_000_00,
    outstandingMinor: 40_000_00,
    numberOfEmis: 3,
    frequency: "MONTHLY",
    status: "ACTIVE",
    emis: [
      { id: "01J5EM1A2B3C4D5E6F7G8H9I", emiNumber: 1, dueDate: "2026-07-15", amountMinor: 20_000_00, paidMinor: 20_000_00, status: "PAID" },
      { id: "01J5EM2B3C4D5E6F7G8H9I0J", emiNumber: 2, dueDate: "2026-08-15", amountMinor: 20_000_00, paidMinor: 0, status: "DUE" },
      { id: "01J5EM3C4D5E6F7G8H9I0J1K", emiNumber: 3, dueDate: "2026-09-15", amountMinor: 20_000_00, paidMinor: 0, status: "UPCOMING" },
    ],
  },
];

export const MOCK_INSTALLMENTS_PAYABLE = [
  {
    id: "01J5IS2B3C4D5E6F7G8H9I0J",
    type: "PAYABLE" as const,
    counterpartyId: "01J5VE2B3C4D5E6F7G8H9I0J",
    counterpartyName: "Gupta Office Supplies",
    sourceType: "PURCHASE",
    sourceRef: "PUR-2026-018",
    totalAmountMinor: 36_000_00,
    outstandingMinor: 24_000_00,
    numberOfEmis: 3,
    frequency: "MONTHLY",
    status: "ACTIVE",
    emis: [
      { id: "01J5EM4D5E6F7G8H9I0J1K2L", emiNumber: 1, dueDate: "2026-07-10", amountMinor: 12_000_00, paidMinor: 12_000_00, status: "PAID" },
      { id: "01J5EM5E6F7G8H9I0J1K2L3M", emiNumber: 2, dueDate: "2026-08-10", amountMinor: 12_000_00, paidMinor: 0, status: "DUE" },
      { id: "01J5EM6F7G8H9I0J1K2L3M4N", emiNumber: 3, dueDate: "2026-09-10", amountMinor: 12_000_00, paidMinor: 0, status: "UPCOMING" },
    ],
  },
];

// ---------------------------------------------------------------------------
// Reminders — matches NotificationDtos.ReminderResponse, CountResponse
// ---------------------------------------------------------------------------

export const MOCK_REMINDERS = [
  {
    id: "01J5RM1A2B3C4D5E6F7G8H9I",
    sourceType: "COMMITMENT",
    sourceRef: "01J5CM1A2B3C4D5E6F7G8H9I",
    emiNumber: null as number | null,
    channel: "SMS",
    templateCode: "PAYMENT_DUE_REMINDER",
    recipient: "9876543210",
    dueDate: "2026-07-30",
    sendOn: "2026-07-27",
    offsetDays: 3,
    status: "SCHEDULED",
  },
];

// ---------------------------------------------------------------------------
// OCR — matches OcrDtos.OcrJobResponse, UploadReservationResponse
// ---------------------------------------------------------------------------

export const MOCK_OCR_JOBS = [
  {
    id: "01J5OC1A2B3C4D5E6F7G8H9I",
    documentId: "01J5DC1A2B3C4D5E6F7G8H9I",
    documentType: "BANK_CHEQUE",
    status: "COMPLETED",
    fields: {
      accountNumber: { value: "50100012345678", source: "OCR", confidence: 0.94 },
      ifsc: { value: "HDFC0001234", source: "OCR", confidence: 0.97 },
      bankName: { value: "HDFC Bank", source: "OCR", confidence: 0.99 },
      holderName: { value: "Sharma Packaging Pvt Ltd", source: "OCR", confidence: 0.88 },
    },
    confidence: "0.92",
    failureReason: null,
    createdAt: "2026-07-20T11:00:00Z",
    updatedAt: "2026-07-20T11:00:05Z",
    reviewedAt: null,
    reviewedBy: null,
  },
];

// ---------------------------------------------------------------------------
// Notifications (push registration) — matches NotificationDtos.DeviceResponse
// ---------------------------------------------------------------------------

export const MOCK_DEVICE_REGISTRATION = {
  id: "01J5DV1A2B3C4D5E6F7G8H9I",
  platform: "web",
  active: true,
};

// ---------------------------------------------------------------------------
// Business / Tenant — matches BusinessDtos.BusinessResponse
// ---------------------------------------------------------------------------

export const MOCK_BUSINESS = {
  id: "01J5TN1A2B3C4D5E6F7G8H9I",
  ownerUserId: "01J5US1A2B3C4D5E6F7G8H9I",
  legalName: "Demo MSME Solutions Pvt Ltd",
  tradeName: "PayWithEase Demo",
  businessType: "PRIVATE_LIMITED",
  gstin: "27AABCD1234E1Z5",
  pan: "AABCD1234E",
  udyam: "UDYAM-MH-01-0012345",
  stateCode: "27",
  status: "ACTIVE",
};

// ---------------------------------------------------------------------------
// Auth mock responses — matches AuthDtos records
// ---------------------------------------------------------------------------

/** Matches the frontend OtpRequestResult (derived from AuthController#requestOtp). */
export const MOCK_OTP_RESULT = {
  challengeId: "01J5CH1A2B3C4D5E6F7G8H9I",
  expiresInSeconds: 300,
  otp: "123456", // dev mode shows OTP
};

/**
 * Matches the frontend TokenPair shape. The backend returns AuthDtos.AuthResponse with
 * { userId, tenantId, roles, tokenType, accessToken, expiresIn, refreshToken, sessionId, newUser }.
 * The frontend auth.ts maps: accessToken, refreshToken, expiresInSeconds ← expiresIn,
 * tenantId, newUser.
 */
export const MOCK_TOKEN_PAIR = {
  accessToken: "demo-access-token-01J5AT1A2B3C4D5E6F7G8H9I",
  refreshToken: "demo-refresh-token-01J5RT1A2B3C4D5E6F7G8H9I",
  expiresInSeconds: 3600,
  tenantId: "01J5TN1A2B3C4D5E6F7G8H9I",
  newUser: false,
};

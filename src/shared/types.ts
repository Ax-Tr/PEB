/**
 * Domain types mirroring the PEB backend API responses. Money fields are integer paise (see
 * money.ts). Kept hand-written and minimal here; a generated OpenAPI client can replace these later.
 */
import type { Minor } from "./money";

/** RFC-7807 problem+json body returned by the backend on errors. */
export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string; // stable PEB error code (e.g. VALIDATION_FAILED)
}

// --- Auth (identity-service) ---
export interface OtpRequestResult {
  challengeId: string;
  expiresInSeconds: number;
}
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

// --- Analytics (analytics-service) ---
export interface Pnl {
  revenueMinor: Minor;
  directCostMinor: Minor;
  operatingExpenseMinor: Minor;
  grossProfitMinor: Minor;
  netProfitMinor: Minor;
  grossMarginPct: string;
  netMarginPct: string;
}
export interface AgingBucket {
  bucket: "DAYS_0_30" | "DAYS_31_60" | "DAYS_61_90" | "DAYS_90_PLUS";
  totalMinor: Minor;
  count: number;
}
export interface Aging {
  buckets: AgingBucket[];
  totalOutstandingMinor: Minor;
  totalCount: number;
}
export interface CashflowPeriod {
  year: number;
  month: number;
  inflowMinor: Minor;
  outflowMinor: Minor;
  netMinor: Minor;
  closingBalanceMinor: Minor;
}
export interface Cashflow {
  openingBalanceMinor: Minor;
  totalInflowMinor: Minor;
  totalOutflowMinor: Minor;
  netMinor: Minor;
  closingBalanceMinor: Minor;
  periods: CashflowPeriod[];
}
export interface StreamFreshness {
  stream: string;
  state: "FRESH" | "STALE" | "NO_DATA";
  lagSeconds: number;
  lastProcessedAt: string | null;
}
export interface ProductProfitability {
  productId: string;
  productName: string;
  revenueMinor: Minor;
  costMinor: Minor;
  profitMinor: Minor;
  marginPct: string;
}

// --- Privacy / DPDP (privacy-service) ---
export interface DsrRequest {
  id: string;
  type: string; // ACCESS|CORRECTION|ERASURE|PORTABILITY|GRIEVANCE
  status: string; // RECEIVED|VERIFYING|IN_PROGRESS|COMPLETED|REJECTED
  subjectRef?: string | null;
  subjectEmail: string;
  details?: string | null;
  erasurePlan?: unknown;
  resolutionNote?: string | null;
  evidenceRef?: string | null;
  dueAt: string;
}
export interface ErasurePlanLine {
  category: string;
  action: string; // DELETE|ANONYMIZE|RETAIN_LEGAL_HOLD
  minRetentionYears: number;
  reason: string;
}
export interface ErasurePlanResult {
  lines: ErasurePlanLine[];
  fullErasurePossible: boolean;
  summary: string;
}

// --- AI automation (ai-automation-service) ---
export interface AiSuggestion {
  id: string;
  kind: string;
  subjectType: string;
  subjectId?: string | null;
  suggestion: unknown; // JSON payload (shape varies by kind)
  confidence: string; // BigDecimal serialised
  decision: string; // AUTO_APPLY|NEEDS_REVIEW|REJECT
  status: string; // PROPOSED|ACCEPTED|REJECTED|AUTO_APPLIED
  modelRef?: string | null;
}
export interface AnomalyAlert {
  id: string;
  subjectType: string;
  subjectId?: string | null;
  observedMinor: Minor;
  score: string;
  severity: string; // LOW|MEDIUM|HIGH
  status: string; // OPEN|ACKNOWLEDGED|DISMISSED
  detail?: string | null;
}
export interface AssistantAnswer {
  answer: string;
  confidence: number;
  modelAvailable: boolean;
  injectionDetected: boolean;
}

// --- Payment collection (payment-collection-service) ---
export interface PaymentRequest {
  paymentRequestId: string;
  reference: string;
  status: string;
  amountMinor: Minor;
  upiUri?: string | null;
}

// --- Invoices (invoice-gst-service) ---
export interface InvoiceLineInput {
  productId?: string;
  description: string;
  hsnSac?: string;
  quantity: number;
  unitPriceMinor: Minor;
  discountMinor: Minor;
  gstRate: number; // percent, e.g. 18
}
export interface CreateInvoiceRequest {
  documentType: string; // "INVOICE"
  supplyType: string; // "B2B" | "B2C"
  customerId?: string;
  customerName?: string;
  customerGstin?: string;
  placeOfSupply?: string;
  businessStateCode?: string;
  reverseCharge: boolean;
  invoiceDate?: string; // ISO date
  lines: InvoiceLineInput[];
}
export interface InvoiceResponse {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  customerName?: string;
  totalTaxableMinor: Minor;
  totalTaxMinor: Minor;
  totalAmountMinor: Minor;
  status: string;
}

// --- Payout (payout-service) ---
export interface CreatePayoutRequest {
  partyType: string; // "VENDOR" | "EMPLOYEE"
  partyId: string;
  beneficiaryId: string;
  amountMinor: Minor;
  purpose?: string;
}
export interface CreatePayoutResponse {
  payoutId: string;
  status: string;
  riskLevel: string; // LOW | MEDIUM | HIGH
  requiresApproval: boolean;
}
export interface PayoutResponse {
  id: string;
  partyType: string;
  partyId: string;
  beneficiaryId: string;
  amountMinor: Minor;
  status: string;
  riskLevel: string;
  provider?: string | null;
  providerRef?: string | null;
}

// --- Reconciliation (reconciliation-service) ---
export interface MatchResponse {
  id: string;
  externalItemId: string;
  internalItemId: string;
  status: string;
}
export interface ReconRunResponse {
  autoMatched: number;
  suggested: number;
  exceptionsCreated: number;
}

// --- Compliance (compliance-report-service) ---
export interface ComplianceReport {
  id: string;
  type: string;
  year: number;
  month: number;
  status: string; // DRAFT|REVIEWED|APPROVED|FILED
  displayState: string; // UNRECONCILED|DRAFT|REVIEWED|APPROVED|FILED
  dataReconciled: boolean;
  totalTaxableMinor: Minor;
  totalTaxMinor: Minor;
  netPayableMinor: Minor;
  missingFields: string[];
  ackReference?: string | null;
}
export interface ComplianceReportLine {
  id: string;
  label: string;
  taxableMinor: Minor;
  taxMinor: Minor;
  amountMinor: Minor;
}

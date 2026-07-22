/**
 * Demo-mode HTTP client with **in-memory persistence**. Implements the exact same interface
 * as {@link HttpClient} (get / post / del). All creates/saves/mutations persist during the
 * browser session and are visible in subsequent GETs. Resets on page refresh.
 *
 * Drop-in replacement: swap `http` in api.ts and every useQuery / useMutation hook works
 * unchanged — same paths, same response shapes, zero UI changes needed.
 */
import type { HttpOptions } from "./http";
import {
  MOCK_AI_ANOMALIES,
  MOCK_AI_SUGGESTIONS,
  MOCK_ASSISTANT_ANSWER,
  MOCK_BANK_ACCOUNTS,
  MOCK_BROKEN_PROMISES,
  MOCK_BUSINESS,
  MOCK_CASHFLOW,
  MOCK_COLLECTION_EFFICIENCY,
  MOCK_COMMITMENT_DETAIL,
  MOCK_COMMITMENT_SUMMARY,
  MOCK_COMMITMENTS,
  MOCK_COMPLIANCE_REPORT_LINES,
  MOCK_COMPLIANCE_REPORTS,
  MOCK_CUSTOMERS,
  MOCK_DEVICE_REGISTRATION,
  MOCK_DSR_REQUESTS,
  MOCK_ERASURE_PLAN,
  MOCK_FRESHNESS,
  MOCK_INSTALLMENTS,
  MOCK_INSTALLMENTS_PAYABLE,
  MOCK_INVOICES,
  MOCK_OCR_JOBS,
  MOCK_OTP_RESULT,
  MOCK_PAYABLES_AGING,
  MOCK_PAYMENT_REQUEST,
  MOCK_PAYOUTS,
  MOCK_PNL,
  MOCK_PRODUCT_PROFITABILITY,
  MOCK_RECEIVABLES_AGING,
  MOCK_RECON_RUN,
  MOCK_RECON_SUGGESTIONS,
  MOCK_REMINDERS,
  MOCK_TOKEN_PAIR,
  MOCK_UPCOMING_OBLIGATIONS,
  MOCK_VENDOR_LIST,
  MOCK_VOICE_DRAFTS,
} from "./mockData";

const DEMO_DELAY_MS = 150;

/** Simulate a brief network delay so the UI loading states render naturally. */
function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), DEMO_DELAY_MS));
}

/** Generate a ULID-like unique ID for new entities. */
function newId(): string {
  const ts = Date.now().toString(36).toUpperCase().padStart(10, "0");
  const rand = Array.from({ length: 16 }, () => "0123456789ABCDEFGHJKMNPQRSTVWXYZ"[Math.floor(Math.random() * 32)]).join("");
  return ts + rand;
}

function today(): string {
  return new Date().toISOString().split("T")[0];
}

function nowISO(): string {
  return new Date().toISOString();
}

// ---------------------------------------------------------------------------
// In-memory store — seeded from mockData, mutated by POST/DELETE operations.
// Each collection is a mutable array; GETs read from these, POSTs push into them.
// ---------------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyRecord = Record<string, any>;

const store = {
  invoices: [...MOCK_INVOICES] as AnyRecord[],
  payouts: [...MOCK_PAYOUTS] as AnyRecord[],
  customers: [...MOCK_CUSTOMERS] as AnyRecord[],
  vendors: { ...MOCK_VENDOR_LIST } as { vendors: AnyRecord[] },
  bankAccounts: { ...MOCK_BANK_ACCOUNTS } as { bankAccounts: AnyRecord[] },
  commitments: [...MOCK_COMMITMENTS] as AnyRecord[],
  installments: [...MOCK_INSTALLMENTS] as AnyRecord[],
  installmentsPayable: [...MOCK_INSTALLMENTS_PAYABLE] as AnyRecord[],
  complianceReports: [...MOCK_COMPLIANCE_REPORTS] as AnyRecord[],
  dsrRequests: [...MOCK_DSR_REQUESTS] as AnyRecord[],
  aiSuggestions: [...MOCK_AI_SUGGESTIONS] as AnyRecord[],
  aiAnomalies: [...MOCK_AI_ANOMALIES] as AnyRecord[],
  voiceDrafts: [...MOCK_VOICE_DRAFTS] as AnyRecord[],
  reconSuggestions: [...MOCK_RECON_SUGGESTIONS] as AnyRecord[],
  reminders: [...MOCK_REMINDERS] as AnyRecord[],
  ocrJobs: [...MOCK_OCR_JOBS] as AnyRecord[],
  paymentRequests: [{ ...MOCK_PAYMENT_REQUEST }] as AnyRecord[],
};

// ---------------------------------------------------------------------------
// GET resolver — reads from in-memory store
// ---------------------------------------------------------------------------

function resolveGet(path: string): unknown {
  const p = path.split("?")[0];

  // --- Analytics (read-only projections — no store needed) ---
  if (p.endsWith("/analytics/pnl")) return MOCK_PNL;
  if (p.endsWith("/analytics/cashflow")) return MOCK_CASHFLOW;
  if (p.endsWith("/analytics/receivables-aging")) return MOCK_RECEIVABLES_AGING;
  if (p.endsWith("/analytics/payables-aging")) return MOCK_PAYABLES_AGING;
  if (p.endsWith("/analytics/product-profitability")) return MOCK_PRODUCT_PROFITABILITY;
  if (p.endsWith("/analytics/freshness")) return MOCK_FRESHNESS;
  if (p.endsWith("/analytics/commitments-summary")) return MOCK_COMMITMENT_SUMMARY;
  if (p.endsWith("/analytics/collection-efficiency")) return MOCK_COLLECTION_EFFICIENCY;
  if (p.endsWith("/analytics/broken-promises")) return MOCK_BROKEN_PROMISES;
  if (p.endsWith("/analytics/upcoming-obligations")) return MOCK_UPCOMING_OBLIGATIONS;

  // --- Invoices ---
  if (p.endsWith("/invoices")) return store.invoices;
  if (p.match(/\/invoices\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.invoices.find((i) => i.id === id) ?? store.invoices[0];
  }

  // --- Payouts ---
  if (p.endsWith("/payouts")) return store.payouts;
  if (p.match(/\/payouts\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.payouts.find((po) => po.id === id) ?? store.payouts[0];
  }

  // --- Payment requests ---
  if (p.match(/\/payment-requests\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.paymentRequests.find((pr) => pr.requestId === id) ?? store.paymentRequests[0];
  }

  // --- Reconciliation ---
  if (p.endsWith("/reconciliation/suggestions")) return store.reconSuggestions;

  // --- Compliance ---
  if (p.endsWith("/compliance/reports")) return store.complianceReports;
  if (p.match(/\/compliance\/reports\/([^/]+)\/lines$/)) return MOCK_COMPLIANCE_REPORT_LINES;
  if (p.match(/\/compliance\/reports\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.complianceReports.find((r) => r.id === id) ?? store.complianceReports[0];
  }

  // --- Privacy ---
  if (p.endsWith("/privacy/requests")) return store.dsrRequests;
  if (p.match(/\/privacy\/requests\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.dsrRequests.find((r) => r.id === id) ?? store.dsrRequests[0];
  }

  // --- AI ---
  if (p.includes("/ai/suggestions")) return store.aiSuggestions;
  if (p.includes("/ai/anomalies")) return store.aiAnomalies;
  if (p.includes("/ai/voice/drafts")) return store.voiceDrafts;

  // --- Commitments ---
  if (p.endsWith("/commitments/due-soon")) return store.commitments.filter((c) => c.status === "OPEN");
  if (p.endsWith("/commitments")) return store.commitments;
  if (p.match(/\/commitments\/([^/]+)$/)) {
    const id = p.split("/").pop();
    const commitment = store.commitments.find((c) => c.id === id) ?? store.commitments[0];
    return { commitment, events: MOCK_COMMITMENT_DETAIL.events };
  }

  // --- Installments ---
  if (p.endsWith("/installments")) {
    const query = path.split("?")[1] ?? "";
    return query.includes("type=PAYABLE") ? store.installmentsPayable : store.installments;
  }
  if (p.match(/\/installments\/([^/]+)$/)) {
    const id = p.split("/").pop();
    return store.installments.find((i) => i.id === id) ?? store.installments[0];
  }

  // --- Masters ---
  if (p.endsWith("/customers")) return store.customers;
  if (p.endsWith("/vendors")) return store.vendors;
  if (p.match(/\/vendors\/[^/]+\/bank-accounts$/)) return store.bankAccounts;

  // --- Reminders ---
  if (p.includes("/reminders")) return store.reminders;

  // --- OCR ---
  if (p.endsWith("/ocr/jobs")) return store.ocrJobs;

  // --- Notifications (devices) ---
  if (p.endsWith("/notifications/devices")) return [MOCK_DEVICE_REGISTRATION];

  // --- Business ---
  if (p.match(/\/businesses\/[^/]+$/)) return MOCK_BUSINESS;

  console.warn(`[DemoHttp] No mock for GET ${path}`);
  return {};
}

// ---------------------------------------------------------------------------
// POST resolver — mutates the in-memory store and returns the new/updated entity
// ---------------------------------------------------------------------------

function resolvePost(path: string, body: unknown): unknown {
  const p = path.split("?")[0];
  const b = (body ?? {}) as AnyRecord;

  // --- Auth ---
  if (p.endsWith("/auth/otp/request")) return MOCK_OTP_RESULT;
  if (p.endsWith("/auth/otp/verify")) return MOCK_TOKEN_PAIR;
  if (p.endsWith("/auth/token/refresh")) return MOCK_TOKEN_PAIR;
  if (p.endsWith("/auth/logout")) return undefined;
  if (p.endsWith("/auth/link-tenant")) return undefined;

  // --- Payment requests ---
  if (p.endsWith("/payment-requests")) {
    const pr = {
      requestId: newId(),
      reference: `PEB-PR-${today().replace(/-/g, "")}-${String(store.paymentRequests.length + 1).padStart(3, "0")}`,
      amountMinor: b.amountMinor ?? 0,
      amountPaidMinor: 0,
      status: "CREATED",
      upiUri: `upi://pay?pa=demo@peb&pn=PayWithEase&am=${((b.amountMinor ?? 0) / 100).toFixed(2)}&cu=INR`,
      paymentLink: `https://pay.paywithease.in/pr/${newId()}`,
    };
    store.paymentRequests.push(pr);
    return pr;
  }
  if (p.match(/\/payment-requests\/[^/]+\/simulate-payment$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const pr = store.paymentRequests.find((r) => r.requestId === id);
    if (pr) { pr.status = "PAID"; pr.amountPaidMinor = pr.amountMinor; }
    return pr ?? { ...MOCK_PAYMENT_REQUEST, status: "PAID", amountPaidMinor: MOCK_PAYMENT_REQUEST.amountMinor };
  }

  // --- Payouts ---
  if (p.endsWith("/payouts")) {
    const payout: AnyRecord = {
      id: newId(), partyType: b.partyType ?? "VENDOR", partyId: b.partyId ?? "",
      beneficiaryId: b.beneficiaryId ?? "", amountMinor: b.amountMinor ?? 0,
      status: "PENDING_APPROVAL", riskLevel: (b.amountMinor ?? 0) > 100_000_00 ? "HIGH" : "LOW",
      provider: null, providerRef: null,
    };
    store.payouts.unshift(payout);
    return { payoutId: payout.id, status: payout.status, riskLevel: payout.riskLevel, requiresApproval: true };
  }
  if (p.match(/\/payouts\/([^/]+)\/approve$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const po = store.payouts.find((x) => x.id === id);
    if (po) { po.status = "COMPLETED"; po.provider = "RAZORPAY"; po.providerRef = `pout_${newId().slice(0, 10)}`; }
    return po ?? store.payouts[0];
  }
  if (p.match(/\/payouts\/([^/]+)\/reject$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const po = store.payouts.find((x) => x.id === id);
    if (po) po.status = "REJECTED";
    return po ?? { ...store.payouts[0], status: "REJECTED" };
  }

  // --- Invoices ---
  if (p.endsWith("/invoices") || p.endsWith("/credit-notes") || p.endsWith("/debit-notes")) {
    const id = newId();
    const num = `INV/2026-27/${String(store.invoices.length + 1).padStart(3, "0")}`;
    const lines = (b.lines ?? []) as AnyRecord[];
    let totalTaxable = 0;
    const itemDtos = lines.map((l: AnyRecord) => {
      const qty = Number(l.quantity ?? 1);
      const unit = Number(l.unitPriceMinor ?? 0);
      const disc = Number(l.discountMinor ?? 0);
      const taxable = qty * unit - disc;
      totalTaxable += taxable;
      const rate = Number(l.gstRate ?? 18);
      const isIntra = (b.placeOfSupply ?? "27") === (b.businessStateCode ?? "27");
      const cgst = isIntra ? Math.round(taxable * rate / 200) : 0;
      const sgst = cgst;
      const igst = isIntra ? 0 : Math.round(taxable * rate / 100);
      return { productId: l.productId ?? null, description: l.description ?? "", hsnSac: l.hsnSac ?? "", quantity: qty, unitPriceMinor: unit, discountMinor: disc, gstRate: rate, taxableValueMinor: taxable, cgstMinor: cgst, sgstMinor: sgst, igstMinor: igst, lineTotalMinor: taxable + cgst + sgst + igst };
    });
    const totalCgst = itemDtos.reduce((s: number, i: AnyRecord) => s + i.cgstMinor, 0);
    const totalSgst = itemDtos.reduce((s: number, i: AnyRecord) => s + i.sgstMinor, 0);
    const totalIgst = itemDtos.reduce((s: number, i: AnyRecord) => s + i.igstMinor, 0);
    const totalTax = totalCgst + totalSgst + totalIgst;
    const inv: AnyRecord = {
      id, documentType: b.documentType ?? "TAX_INVOICE", supplyType: b.supplyType ?? "B2B",
      invoiceNumber: num, financialYear: "2026-27", invoiceDate: b.invoiceDate ?? today(),
      customerName: b.customerName ?? "New Customer", customerGstin: b.customerGstin ?? null,
      placeOfSupply: b.placeOfSupply ?? "27", reverseCharge: b.reverseCharge ?? false, taxable: true,
      totalTaxableMinor: totalTaxable, totalCgstMinor: totalCgst, totalSgstMinor: totalSgst,
      totalIgstMinor: totalIgst, totalTaxMinor: totalTax, totalAmountMinor: totalTaxable + totalTax,
      status: "CREATED", items: itemDtos,
      taxLines: [{ gstRate: 18, taxableValueMinor: totalTaxable, cgstMinor: totalCgst, sgstMinor: totalSgst, igstMinor: totalIgst }],
    };
    store.invoices.unshift(inv);
    return inv;
  }
  if (p.match(/\/invoices\/([^/]+)\/send$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const inv = store.invoices.find((i) => i.id === id);
    if (inv) inv.status = "SENT";
    return inv ?? store.invoices[0];
  }

  // --- Reconciliation ---
  if (p.endsWith("/reconciliation/run")) return MOCK_RECON_RUN;
  if (p.match(/\/reconciliation\/matches\/([^/]+)\/confirm$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const m = store.reconSuggestions.find((s) => s.id === id);
    if (m) m.status = "CONFIRMED";
    return m ?? { ...MOCK_RECON_SUGGESTIONS[0], status: "CONFIRMED" };
  }
  if (p.match(/\/reconciliation\/matches\/([^/]+)\/reject$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const m = store.reconSuggestions.find((s) => s.id === id);
    if (m) m.status = "REJECTED";
    return m ?? { ...MOCK_RECON_SUGGESTIONS[0], status: "REJECTED" };
  }

  // --- Compliance ---
  if (p.endsWith("/compliance/reports/generate")) return store.complianceReports[0];
  if (p.match(/\/compliance\/reports\/([^/]+)\/(reconciled|review|approve|filing)$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    return store.complianceReports.find((r) => r.id === id) ?? store.complianceReports[0];
  }

  // --- Privacy ---
  if (p.endsWith("/privacy/requests")) {
    const dsr: AnyRecord = {
      id: newId(), type: b.type ?? "ACCESS", status: "RECEIVED",
      subjectRef: b.subjectRef ?? "USR-NEW", subjectEmail: b.subjectEmail ?? "new@example.com",
      details: b.details ?? "", erasurePlan: null, resolutionNote: null,
      evidenceRef: null, dueAt: new Date(Date.now() + 30 * 86400000).toISOString(),
    };
    store.dsrRequests.unshift(dsr);
    return dsr;
  }
  if (p.match(/\/privacy\/requests\/[^/]+\/erasure-plan$/)) return MOCK_ERASURE_PLAN;
  if (p.match(/\/privacy\/requests\/([^/]+)\/(verify|start|complete|reject)$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const action = p.split("/").pop();
    const dsr = store.dsrRequests.find((r) => r.id === id);
    if (dsr) {
      if (action === "verify") dsr.status = "VERIFIED";
      else if (action === "start") dsr.status = "IN_PROGRESS";
      else if (action === "complete") dsr.status = "COMPLETED";
      else if (action === "reject") dsr.status = "REJECTED";
    }
    return dsr ?? store.dsrRequests[0];
  }

  // --- AI ---
  if (p.match(/\/ai\/suggestions\/([^/]+)\/(accept|reject)$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const action = p.split("/").pop();
    const s = store.aiSuggestions.find((x) => x.id === id);
    if (s) { s.decision = action === "accept" ? "ACCEPTED" : "REJECTED"; s.status = action === "accept" ? "APPLIED" : "REJECTED"; }
    return s ?? store.aiSuggestions[0];
  }
  if (p.match(/\/ai\/anomalies\/([^/]+)\/(acknowledge|dismiss)$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const action = p.split("/").pop();
    const a = store.aiAnomalies.find((x) => x.id === id);
    if (a) a.status = action === "acknowledge" ? "ACKNOWLEDGED" : "DISMISSED";
    return a ?? store.aiAnomalies[0];
  }
  if (p.endsWith("/ai/assistant/ask")) return MOCK_ASSISTANT_ANSWER;
  if (p.endsWith("/ai/voice/parse")) {
    const draft: AnyRecord = {
      id: newId(), transcript: b.transcript ?? "", sanitizedTranscript: b.transcript ?? "",
      intent: "CREATE_INVOICE", status: "NEEDS_REVIEW",
      fields: { description: b.transcript ?? "" }, missingFields: ["customerName", "amountMinor"],
      confidence: "0.75", suspicious: false, materializedRef: null, rejectionReason: null,
      createdAt: nowISO(), updatedAt: nowISO(), reviewedAt: null, reviewedBy: null,
    };
    store.voiceDrafts.unshift(draft);
    return draft;
  }
  if (p.match(/\/ai\/voice\/drafts\/([^/]+)\/approve$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const d = store.voiceDrafts.find((x) => x.id === id);
    if (d) { d.status = "APPROVED"; d.reviewedAt = nowISO(); }
    return d ?? { ...MOCK_VOICE_DRAFTS[0], status: "APPROVED" };
  }
  if (p.match(/\/ai\/voice\/drafts\/([^/]+)\/reject$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const d = store.voiceDrafts.find((x) => x.id === id);
    if (d) { d.status = "REJECTED"; d.rejectionReason = b.reason ?? ""; d.reviewedAt = nowISO(); }
    return d ?? { ...MOCK_VOICE_DRAFTS[0], status: "REJECTED" };
  }

  // --- Commitments ---
  if (p.endsWith("/commitments")) {
    const c: AnyRecord = {
      id: newId(), counterpartyType: b.counterpartyType ?? "CUSTOMER",
      counterpartyId: b.counterpartyId ?? null, counterpartyName: b.counterpartyName ?? "New Party",
      sourceType: b.sourceType ?? null, sourceRef: b.sourceRef ?? null,
      description: b.description ?? "", amountMinor: b.amountMinor ?? 0,
      paidMinor: 0, outstandingMinor: b.amountMinor ?? 0,
      dueDate: b.dueDate ?? today(), status: "OPEN",
      createdAt: nowISO(), updatedAt: nowISO(), closedAt: null,
    };
    store.commitments.unshift(c);
    return c;
  }
  if (p.match(/\/commitments\/([^/]+)\/record-payment$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const c = store.commitments.find((x) => x.id === id);
    if (c) {
      c.paidMinor += b.amountMinor ?? 0;
      c.outstandingMinor = Math.max(0, c.amountMinor - c.paidMinor);
      if (c.outstandingMinor === 0) { c.status = "FULFILLED"; c.closedAt = nowISO(); }
      c.updatedAt = nowISO();
    }
    return c ?? store.commitments[0];
  }
  if (p.match(/\/commitments\/([^/]+)\/reschedule$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const c = store.commitments.find((x) => x.id === id);
    if (c) { c.dueDate = b.newDueDate ?? c.dueDate; c.updatedAt = nowISO(); }
    return c ?? store.commitments[0];
  }
  if (p.match(/\/commitments\/([^/]+)\/cancel$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const c = store.commitments.find((x) => x.id === id);
    if (c) { c.status = "CANCELLED"; c.closedAt = nowISO(); c.updatedAt = nowISO(); }
    return c ?? { ...store.commitments[0], status: "CANCELLED" };
  }

  // --- Installments ---
  if (p.endsWith("/installments")) return store.installments[0];
  if (p.match(/\/installments\/[^/]+\/pay$/)) return store.installments[0];
  if (p.match(/\/installments\/[^/]+\/modify$/)) return store.installments[0];
  if (p.match(/\/installments\/[^/]+\/cancel$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const inst = store.installments.find((x) => x.id === id);
    if (inst) inst.status = "CANCELLED";
    return inst ?? { ...store.installments[0], status: "CANCELLED" };
  }

  // --- Customers ---
  if (p.endsWith("/customers")) {
    const cust: AnyRecord = {
      id: newId(), name: b.name ?? "New Customer", mobile: b.mobile ?? "9000000000",
      email: b.email ?? null, address: b.address ?? null, gstin: b.gstin ?? null,
      createdAt: nowISO(),
    };
    store.customers.unshift(cust);
    return cust;
  }

  // --- Vendors ---
  if (p.match(/\/vendors\/[^/]+\/bank-accounts$/)) return store.bankAccounts.bankAccounts[0];

  // --- Reminders ---
  if (p.endsWith("/reminders")) {
    const rem: AnyRecord = {
      id: newId(), sourceType: b.sourceType ?? "COMMITMENT", sourceRef: b.sourceRef ?? "",
      emiNumber: b.emiNumber ?? null, channel: b.channel ?? "SMS",
      templateCode: b.templateCode ?? "PAYMENT_DUE_REMINDER",
      recipient: b.recipient ?? "", dueDate: b.dueDate ?? today(),
      sendOn: b.dueDate ?? today(), offsetDays: 3, status: "SCHEDULED",
    };
    store.reminders.unshift(rem);
    return { count: 1 };
  }

  // --- OCR ---
  if (p.endsWith("/documents/upload-url")) {
    const docId = newId();
    return {
      documentId: docId, storageKey: `demo/uploads/${docId}`,
      uploadUrl: `https://demo.storage.local/upload/${docId}`,
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      document: {
        id: docId, storageKey: `demo/uploads/${docId}`,
        originalFilename: b.filename ?? "document.pdf", mimeType: b.mimeType ?? "application/pdf",
        checksum: b.checksum ?? null, sizeBytes: b.sizeBytes ?? 100000, createdAt: nowISO(),
      },
    };
  }
  if (p.endsWith("/ocr/jobs")) {
    const job: AnyRecord = {
      id: newId(), documentId: b.documentId ?? newId(), documentType: b.documentType ?? "BANK_CHEQUE",
      status: "COMPLETED", fields: {}, confidence: "0.90", failureReason: null,
      createdAt: nowISO(), updatedAt: nowISO(), reviewedAt: null, reviewedBy: null,
    };
    store.ocrJobs.unshift(job);
    return job;
  }
  if (p.match(/\/ocr\/jobs\/([^/]+)\/review$/)) {
    const id = p.split("/").slice(-2, -1)[0];
    const job = store.ocrJobs.find((j) => j.id === id);
    if (job) { job.status = b.accepted ? "ACCEPTED" : "REJECTED"; job.reviewedAt = nowISO(); }
    return job ?? { ...store.ocrJobs[0], status: "REVIEWED" };
  }

  // --- Notifications (push) ---
  if (p.endsWith("/notifications/devices")) return MOCK_DEVICE_REGISTRATION;

  // --- Business ---
  if (p.endsWith("/businesses")) return MOCK_BUSINESS;

  console.warn(`[DemoHttp] No mock for POST ${path}`);
  return {};
}

// ---------------------------------------------------------------------------
// DELETE resolver
// ---------------------------------------------------------------------------

function resolveDel(path: string): unknown {
  const p = path.split("?")[0];

  // Delete sessions
  if (p.match(/\/auth\/sessions\/[^/]+$/)) return undefined;

  // Generic: try to remove from known collections
  const segments = p.split("/");
  const id = segments.pop();
  if (id) {
    // Try to remove from any collection that matches
    for (const [_key, arr] of Object.entries(store)) {
      if (Array.isArray(arr)) {
        const idx = arr.findIndex((item: AnyRecord) => item.id === id || item.requestId === id);
        if (idx !== -1) {
          arr.splice(idx, 1);
          return undefined;
        }
      }
    }
  }

  return undefined; // 204 No Content
}

/**
 * Demo HTTP client with in-memory persistence. Implements the same public interface
 * as {@link HttpClient}. Creates/saves persist in the session; lists reflect mutations.
 */
export class DemoHttpClient {
  get<T>(path: string, _opts?: HttpOptions): Promise<T> {
    return delay(resolveGet(path) as T);
  }

  post<T>(path: string, body?: unknown, _opts?: HttpOptions): Promise<T> {
    return delay(resolvePost(path, body) as T);
  }

  del<T>(path: string, _opts?: HttpOptions): Promise<T> {
    return delay(resolveDel(path) as T);
  }
}

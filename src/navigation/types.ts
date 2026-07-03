/** Route params for the "Books" stack (accounting/compliance journeys). */
export type BooksStackParamList = {
  BooksMenu: undefined;
  InvoiceList: undefined;
  Invoices: undefined; // create
  PayoutList: undefined;
  CommitmentList: undefined;
  CommitmentCreate: undefined;
  CommitmentDetail: { commitmentId: string; title: string };
  InstallmentList: undefined;
  InstallmentCreate: { type?: "RECEIVABLE" | "PAYABLE" } | undefined;
  InstallmentDetail: { installmentId: string; title: string };
  ReminderList: undefined;
  ReminderCreate: { sourceRef?: string; sourceType?: string; emiNumber?: number; dueDate?: string } | undefined;
  BankOcrCapture: undefined;
  Reconciliation: undefined;
  Compliance: undefined;
  ComplianceDetail: { reportId: string; title: string };
};

/** Route params for the "More" stack (insights, data rights, AI assistant). */
export type MoreStackParamList = {
  MoreMenu: undefined;
  Insights: undefined;
  DataRights: undefined;
  DataRightDetail: { requestId: string };
  Assistant: undefined;
  VoiceDraftReview: { draftId?: string } | undefined;
};

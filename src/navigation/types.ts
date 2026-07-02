/** Route params for the "Books" stack (accounting/compliance journeys). */
export type BooksStackParamList = {
  BooksMenu: undefined;
  Invoices: undefined;
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
};

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { ComplianceReport, ComplianceReportLine } from "../../shared/types";

const LIST_KEY = ["compliance", "reports"];

export function useReports() {
  return useQuery({
    queryKey: LIST_KEY,
    queryFn: () => http.get<ComplianceReport[]>(`${API_PREFIX}/compliance/reports`),
  });
}

export function useReport(id: string) {
  return useQuery({
    queryKey: ["compliance", "report", id],
    queryFn: () => http.get<ComplianceReport>(`${API_PREFIX}/compliance/reports/${id}`),
  });
}

export function useReportLines(id: string) {
  return useQuery({
    queryKey: ["compliance", "report", id, "lines"],
    queryFn: () => http.get<ComplianceReportLine[]>(`${API_PREFIX}/compliance/reports/${id}/lines`),
  });
}

export interface GenerateInput {
  type: string;
  year: number;
  month: number;
}

export function useGenerateReport() {
  const qc = useQueryClient();
  return useMutation<ComplianceReport, unknown, GenerateInput>({
    mutationFn: (input) => http.post<ComplianceReport>(`${API_PREFIX}/compliance/reports/generate`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: LIST_KEY }),
  });
}

function useReportAction<TBody>(path: (id: string) => string) {
  const qc = useQueryClient();
  return useMutation<ComplianceReport, unknown, { id: string; body?: TBody }>({
    mutationFn: ({ id, body }) => http.post<ComplianceReport>(path(id), body),
    onSuccess: (r) => {
      qc.setQueryData(["compliance", "report", r.id], r);
      void qc.invalidateQueries({ queryKey: LIST_KEY });
    },
  });
}

export const useSetReconciled = () =>
  useReportAction<{ reconciled: boolean }>((id) => `${API_PREFIX}/compliance/reports/${id}/reconciled`);
export const useReviewReport = () =>
  useReportAction((id) => `${API_PREFIX}/compliance/reports/${id}/review`);
export const useApproveReport = () =>
  useReportAction((id) => `${API_PREFIX}/compliance/reports/${id}/approve`);
export const useRecordFiling = () =>
  useReportAction<{ ackReference: string }>((id) => `${API_PREFIX}/compliance/reports/${id}/filing`);

export const REPORT_TYPES = [
  "GSTR3B_SUMMARY",
  "GSTR1_SUMMARY",
  "SALES_REGISTER",
  "PURCHASE_REGISTER",
  "TDS_SUMMARY",
  "PAYROLL_COMPLIANCE",
] as const;

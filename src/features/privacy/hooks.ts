import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { DsrRequest, ErasurePlanResult } from "../../shared/types";

const LIST_KEY = ["privacy", "requests"];

export function useDsrRequests() {
  return useQuery({
    queryKey: LIST_KEY,
    queryFn: () => http.get<DsrRequest[]>(`${API_PREFIX}/privacy/requests`),
  });
}

export function useDsrRequest(id: string) {
  return useQuery({
    queryKey: ["privacy", "request", id],
    queryFn: () => http.get<DsrRequest>(`${API_PREFIX}/privacy/requests/${id}`),
  });
}

export interface SubmitDsrInput {
  type: string;
  subjectEmail: string;
  subjectRef?: string;
  details?: string;
}

export function useSubmitDsr() {
  const qc = useQueryClient();
  return useMutation<DsrRequest, unknown, SubmitDsrInput>({
    mutationFn: (input) => http.post<DsrRequest>(`${API_PREFIX}/privacy/requests`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: LIST_KEY }),
  });
}

function useDsrAction() {
  const qc = useQueryClient();
  return useMutation<DsrRequest, unknown, { id: string; action: string; body?: unknown }>({
    mutationFn: ({ id, action, body }) =>
      http.post<DsrRequest>(`${API_PREFIX}/privacy/requests/${id}/${action}`, body),
    onSuccess: (r) => {
      qc.setQueryData(["privacy", "request", r.id], r);
      void qc.invalidateQueries({ queryKey: LIST_KEY });
    },
  });
}

export const useDsrLifecycle = useDsrAction;

export function useErasurePlan() {
  return useMutation<ErasurePlanResult, unknown, { id: string; categories: string[] }>({
    mutationFn: ({ id, categories }) =>
      http.post<ErasurePlanResult>(`${API_PREFIX}/privacy/requests/${id}/erasure-plan`, { categories }),
  });
}

export const DSR_TYPES = ["ACCESS", "CORRECTION", "ERASURE", "PORTABILITY", "GRIEVANCE"] as const;

export const DATA_CATEGORIES = [
  "PROFILE_PII",
  "CONTACT_PII",
  "KYC_PII",
  "FINANCIAL_TXN",
  "TAX_RECORD",
  "MARKETING",
  "AUDIT_TRAIL",
] as const;

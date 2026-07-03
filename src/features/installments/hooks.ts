import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type {
  CreateInstallmentRequest,
  Installment,
  ModifyInstallmentRequest,
  PayInstallmentEmiRequest,
} from "../../shared/types";

const INSTALLMENTS_KEY = ["installments"];

export function useInstallments(type: "RECEIVABLE" | "PAYABLE") {
  return useQuery({
    queryKey: [...INSTALLMENTS_KEY, type],
    queryFn: () => http.get<Installment[]>(`${API_PREFIX}/installments?type=${type}`),
  });
}

export function useInstallment(id: string) {
  return useQuery({
    queryKey: [...INSTALLMENTS_KEY, id],
    queryFn: () => http.get<Installment>(`${API_PREFIX}/installments/${id}`),
  });
}

export function useCreateInstallment() {
  const qc = useQueryClient();
  return useMutation<Installment, unknown, CreateInstallmentRequest>({
    mutationFn: (input) => http.post<Installment>(`${API_PREFIX}/installments`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: INSTALLMENTS_KEY }),
  });
}

export function usePayInstallmentEmi(id: string) {
  const qc = useQueryClient();
  return useMutation<Installment, unknown, PayInstallmentEmiRequest>({
    mutationFn: (input) => http.post<Installment>(`${API_PREFIX}/installments/${id}/pay`, input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: INSTALLMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...INSTALLMENTS_KEY, id] });
    },
  });
}

export function useModifyInstallment(id: string) {
  const qc = useQueryClient();
  return useMutation<Installment, unknown, ModifyInstallmentRequest>({
    mutationFn: (input) => http.post<Installment>(`${API_PREFIX}/installments/${id}/modify`, input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: INSTALLMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...INSTALLMENTS_KEY, id] });
    },
  });
}

export function useCancelInstallment(id: string) {
  const qc = useQueryClient();
  return useMutation<Installment, unknown, void>({
    mutationFn: () => http.post<Installment>(`${API_PREFIX}/installments/${id}/cancel`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: INSTALLMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...INSTALLMENTS_KEY, id] });
    },
  });
}

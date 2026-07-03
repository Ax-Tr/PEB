import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import { cryptoRandomId } from "../../shared/http";
import type {
  Commitment,
  CommitmentDetail,
  CreateCommitmentRequest,
  RecordCommitmentPaymentRequest,
  RescheduleCommitmentRequest,
} from "../../shared/types";

const COMMITMENTS_KEY = ["commitments"];

export function useCommitments(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return useQuery({
    queryKey: [...COMMITMENTS_KEY, status ?? "all"],
    queryFn: () => http.get<Commitment[]>(`${API_PREFIX}/commitments${query}`),
  });
}

export function useDueSoonCommitments(days = 7) {
  return useQuery({
    queryKey: [...COMMITMENTS_KEY, "due-soon", days],
    queryFn: () => http.get<Commitment[]>(`${API_PREFIX}/commitments/due-soon?days=${days}`),
  });
}

export function useCommitment(id: string) {
  return useQuery({
    queryKey: [...COMMITMENTS_KEY, id],
    queryFn: () => http.get<CommitmentDetail>(`${API_PREFIX}/commitments/${id}`),
  });
}

export function useCreateCommitment() {
  const qc = useQueryClient();
  return useMutation<Commitment, unknown, CreateCommitmentRequest>({
    mutationFn: (input) =>
      http.post<Commitment>(`${API_PREFIX}/commitments`, input, { idempotencyKey: cryptoRandomId() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: COMMITMENTS_KEY }),
  });
}

export function useRecordCommitmentPayment(id: string) {
  const qc = useQueryClient();
  return useMutation<Commitment, unknown, RecordCommitmentPaymentRequest>({
    mutationFn: (input) =>
      http.post<Commitment>(`${API_PREFIX}/commitments/${id}/record-payment`, input, {
        idempotencyKey: cryptoRandomId(),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: COMMITMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...COMMITMENTS_KEY, id] });
    },
  });
}

export function useRescheduleCommitment(id: string) {
  const qc = useQueryClient();
  return useMutation<Commitment, unknown, RescheduleCommitmentRequest>({
    mutationFn: (input) => http.post<Commitment>(`${API_PREFIX}/commitments/${id}/reschedule`, input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: COMMITMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...COMMITMENTS_KEY, id] });
    },
  });
}

export function useCancelCommitment(id: string) {
  const qc = useQueryClient();
  return useMutation<Commitment, unknown, { note?: string }>({
    mutationFn: (input) => http.post<Commitment>(`${API_PREFIX}/commitments/${id}/cancel`, input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: COMMITMENTS_KEY });
      void qc.invalidateQueries({ queryKey: [...COMMITMENTS_KEY, id] });
    },
  });
}

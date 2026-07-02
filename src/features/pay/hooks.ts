import { useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import { cryptoRandomId } from "../../shared/http";
import type { CreatePayoutRequest, CreatePayoutResponse, PayoutResponse } from "../../shared/types";

/**
 * Create a payout. Idempotency-Key guards against a double-submit; {@code stepUpVerified} passes the
 * biometric step-up result so the backend allows a high-risk payout to be created.
 */
export function useCreatePayout() {
  return useMutation<CreatePayoutResponse, unknown, CreatePayoutRequest & { stepUpVerified?: boolean }>({
    mutationFn: ({ stepUpVerified, ...input }) =>
      http.post<CreatePayoutResponse>(`${API_PREFIX}/payouts`, input, {
        idempotencyKey: cryptoRandomId(),
        stepUpVerified,
      }),
  });
}

/** Approve a pending payout (checker; the backend forbids the maker approving their own). */
export function useApprovePayout() {
  const qc = useQueryClient();
  return useMutation<PayoutResponse, unknown, string>({
    mutationFn: (id) => http.post<PayoutResponse>(`${API_PREFIX}/payouts/${id}/approve`),
    onSuccess: (p) => qc.setQueryData(["payout", p.id], p),
  });
}

export function useRejectPayout() {
  const qc = useQueryClient();
  return useMutation<PayoutResponse, unknown, { id: string; reason: string }>({
    mutationFn: ({ id, reason }) =>
      http.post<PayoutResponse>(`${API_PREFIX}/payouts/${id}/reject`, { reason }),
    onSuccess: (p) => qc.setQueryData(["payout", p.id], p),
  });
}

import { useMutation } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/config";
import { cryptoRandomId } from "../../shared/http";
import type { Minor } from "../../shared/money";
import type { PaymentRequest } from "../../shared/types";

export interface CreatePaymentInput {
  amountMinor: Minor;
  reference: string;
}

/** Create a collection request. Sends an Idempotency-Key so a double-tap never double-charges. */
export function useCreatePaymentRequest() {
  return useMutation<PaymentRequest, unknown, CreatePaymentInput>({
    mutationFn: (input) =>
      http.post<PaymentRequest>(`${API_PREFIX}/payment-requests`, input, {
        idempotencyKey: cryptoRandomId(),
      }),
  });
}

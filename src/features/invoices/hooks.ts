import { useMutation } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import { cryptoRandomId } from "../../shared/http";
import type { CreateInvoiceRequest, InvoiceResponse } from "../../shared/types";

export function useCreateInvoice() {
  return useMutation<InvoiceResponse, unknown, CreateInvoiceRequest>({
    mutationFn: (input) =>
      http.post<InvoiceResponse>(`${API_PREFIX}/invoices`, input, {
        idempotencyKey: cryptoRandomId(),
      }),
  });
}

export function useSendInvoice() {
  return useMutation<InvoiceResponse, unknown, { id: string; channel: string }>({
    mutationFn: ({ id, channel }) =>
      http.post<InvoiceResponse>(`${API_PREFIX}/invoices/${id}/send`, { channel }),
  });
}

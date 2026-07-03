import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import { cryptoRandomId } from "../../shared/http";
import type {
  BankAccountList,
  AddVendorBankAccountRequest,
  CreateCustomerRequest,
  Customer,
  BankAccount,
  VendorList,
} from "../../shared/types";

const CUSTOMERS_KEY = ["customers"];

export function useCustomers() {
  return useQuery({
    queryKey: CUSTOMERS_KEY,
    queryFn: () => http.get<Customer[]>(`${API_PREFIX}/customers`),
  });
}

export function useCreateCustomer() {
  const qc = useQueryClient();
  return useMutation<Customer, unknown, CreateCustomerRequest>({
    mutationFn: (input) =>
      http.post<Customer>(`${API_PREFIX}/customers`, input, { idempotencyKey: cryptoRandomId() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: CUSTOMERS_KEY }),
  });
}

export function useVendors() {
  return useQuery({
    queryKey: ["vendors"],
    queryFn: async () => (await http.get<VendorList>(`${API_PREFIX}/vendors`)).vendors,
  });
}

export function useVendorBankAccounts(vendorId: string | null) {
  return useQuery({
    queryKey: ["vendor", vendorId, "bank-accounts"],
    enabled: vendorId != null,
    queryFn: async () =>
      (await http.get<BankAccountList>(`${API_PREFIX}/vendors/${vendorId}/bank-accounts`)).bankAccounts,
  });
}

export function useAddVendorBankAccount(vendorId: string | null) {
  const qc = useQueryClient();
  return useMutation<BankAccount, unknown, AddVendorBankAccountRequest>({
    mutationFn: (input) =>
      http.post<BankAccount>(`${API_PREFIX}/vendors/${vendorId}/bank-accounts`, input, {
        idempotencyKey: cryptoRandomId(),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["vendor", vendorId, "bank-accounts"] });
      qc.invalidateQueries({ queryKey: ["vendors"] });
    },
  });
}

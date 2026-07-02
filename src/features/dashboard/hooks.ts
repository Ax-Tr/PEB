import { useQuery } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/config";
import type { Aging, Cashflow, Pnl, StreamFreshness } from "../../shared/types";

/** Current period in Asia/Kolkata terms (device clock); the backend also reasons in IST. */
export function currentPeriod(): { year: number; month: number } {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function usePnl() {
  const { year, month } = currentPeriod();
  return useQuery({
    queryKey: ["analytics", "pnl", year, month],
    queryFn: () => http.get<Pnl>(`${API_PREFIX}/analytics/pnl?year=${year}&month=${month}`),
  });
}

export function useCashflow() {
  return useQuery({
    queryKey: ["analytics", "cashflow"],
    queryFn: () => http.get<Cashflow>(`${API_PREFIX}/analytics/cashflow`),
  });
}

export function useReceivablesAging() {
  return useQuery({
    queryKey: ["analytics", "receivables-aging"],
    queryFn: () => http.get<Aging>(`${API_PREFIX}/analytics/receivables-aging`),
  });
}

export function useFreshness() {
  return useQuery({
    queryKey: ["analytics", "freshness"],
    queryFn: () => http.get<StreamFreshness[]>(`${API_PREFIX}/analytics/freshness`),
  });
}

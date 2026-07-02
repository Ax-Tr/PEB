import { useQuery } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { Aging, ProductProfitability } from "../../shared/types";
import { currentPeriod } from "./hooks";

export function usePayablesAging() {
  return useQuery({
    queryKey: ["analytics", "payables-aging"],
    queryFn: () => http.get<Aging>(`${API_PREFIX}/analytics/payables-aging`),
  });
}

export function useProductProfitability() {
  const { year, month } = currentPeriod();
  return useQuery({
    queryKey: ["analytics", "product-profitability", year, month],
    queryFn: () =>
      http.get<ProductProfitability[]>(
        `${API_PREFIX}/analytics/product-profitability?year=${year}&month=${month}`,
      ),
  });
}

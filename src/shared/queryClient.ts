/**
 * TanStack Query client for server-state. Does not retry client (4xx) errors — those are actionable
 * (validation/auth), not transient. Short stale time keeps read-model dashboards reasonably fresh.
 */
import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./http";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

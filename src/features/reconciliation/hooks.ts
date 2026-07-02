import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { MatchResponse, ReconRunResponse } from "../../shared/types";

const SUGGESTIONS_KEY = ["reconciliation", "suggestions"];

export function useSuggestions() {
  return useQuery({
    queryKey: SUGGESTIONS_KEY,
    queryFn: () => http.get<MatchResponse[]>(`${API_PREFIX}/reconciliation/suggestions`),
  });
}

export function useRunReconciliation() {
  const qc = useQueryClient();
  return useMutation<ReconRunResponse, unknown, void>({
    mutationFn: () => http.post<ReconRunResponse>(`${API_PREFIX}/reconciliation/run`),
    onSuccess: () => qc.invalidateQueries({ queryKey: SUGGESTIONS_KEY }),
  });
}

export function useConfirmMatch() {
  const qc = useQueryClient();
  return useMutation<MatchResponse, unknown, string>({
    mutationFn: (id) => http.post<MatchResponse>(`${API_PREFIX}/reconciliation/matches/${id}/confirm`),
    onSuccess: () => qc.invalidateQueries({ queryKey: SUGGESTIONS_KEY }),
  });
}

export function useRejectMatch() {
  const qc = useQueryClient();
  return useMutation<MatchResponse, unknown, string>({
    mutationFn: (id) => http.post<MatchResponse>(`${API_PREFIX}/reconciliation/matches/${id}/reject`),
    onSuccess: () => qc.invalidateQueries({ queryKey: SUGGESTIONS_KEY }),
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { AiSuggestion, AnomalyAlert, AssistantAnswer } from "../../shared/types";

const SUGGESTIONS_KEY = ["ai", "suggestions"];
const ANOMALIES_KEY = ["ai", "anomalies"];

export function useAiSuggestions(status = "PROPOSED") {
  return useQuery({
    queryKey: [...SUGGESTIONS_KEY, status],
    queryFn: () => http.get<AiSuggestion[]>(`${API_PREFIX}/ai/suggestions?status=${status}`),
  });
}

export function useDecideSuggestion(action: "accept" | "reject") {
  const qc = useQueryClient();
  return useMutation<AiSuggestion, unknown, string>({
    mutationFn: (id) => http.post<AiSuggestion>(`${API_PREFIX}/ai/suggestions/${id}/${action}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: SUGGESTIONS_KEY }),
  });
}

export function useAnomalies(status = "OPEN") {
  return useQuery({
    queryKey: [...ANOMALIES_KEY, status],
    queryFn: () => http.get<AnomalyAlert[]>(`${API_PREFIX}/ai/anomalies?status=${status}`),
  });
}

export function useDecideAnomaly(action: "acknowledge" | "dismiss") {
  const qc = useQueryClient();
  return useMutation<AnomalyAlert, unknown, string>({
    mutationFn: (id) => http.post<AnomalyAlert>(`${API_PREFIX}/ai/anomalies/${id}/${action}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ANOMALIES_KEY }),
  });
}

export function useAskAssistant() {
  return useMutation<AssistantAnswer, unknown, string>({
    mutationFn: (question) => http.post<AssistantAnswer>(`${API_PREFIX}/ai/assistant/ask`, { question }),
  });
}

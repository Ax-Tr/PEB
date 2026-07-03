import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import type { CountResponse, Reminder, ScheduleReminderRequest } from "../../shared/types";

const REMINDERS_KEY = ["reminders"];

export function useReminders(sourceRef?: string) {
  const query = sourceRef ? `?sourceRef=${encodeURIComponent(sourceRef)}` : "";
  return useQuery({
    queryKey: [...REMINDERS_KEY, sourceRef ?? "all"],
    queryFn: () => http.get<Reminder[]>(`${API_PREFIX}/reminders${query}`),
  });
}

export function useScheduleReminder() {
  const qc = useQueryClient();
  return useMutation<CountResponse, unknown, ScheduleReminderRequest>({
    mutationFn: (input) => http.post<CountResponse>(`${API_PREFIX}/reminders`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: REMINDERS_KEY }),
  });
}

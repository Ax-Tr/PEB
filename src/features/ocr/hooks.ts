import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { http } from "../../shared/api";
import { API_PREFIX } from "../../shared/constants";
import { cryptoRandomId } from "../../shared/http";
import type { OcrJob, OcrUploadReservation, StartOcrJobRequest } from "../../shared/types";

export interface ReserveOcrUploadRequest {
  filename: string;
  mimeType: string;
  checksum?: string;
  sizeBytes: number;
}

export function useOcrJobs() {
  return useQuery({
    queryKey: ["ocr-jobs"],
    queryFn: () => http.get<OcrJob[]>(`${API_PREFIX}/ocr/jobs`),
  });
}

export function useReserveOcrUpload() {
  return useMutation<OcrUploadReservation, unknown, ReserveOcrUploadRequest>({
    mutationFn: (input) =>
      http.post<OcrUploadReservation>(`${API_PREFIX}/documents/upload-url`, input, {
        idempotencyKey: cryptoRandomId(),
      }),
  });
}

export function useStartOcrJob() {
  const qc = useQueryClient();
  return useMutation<OcrJob, unknown, StartOcrJobRequest>({
    mutationFn: (input) =>
      http.post<OcrJob>(`${API_PREFIX}/ocr/jobs`, input, { idempotencyKey: cryptoRandomId() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ocr-jobs"] }),
  });
}

export function useReviewOcrJob() {
  const qc = useQueryClient();
  return useMutation<OcrJob, unknown, { id: string; accepted: boolean; fields?: Record<string, unknown> }>({
    mutationFn: ({ id, accepted, fields }) =>
      http.post<OcrJob>(`${API_PREFIX}/ocr/jobs/${id}/review`, { accepted, fields }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ocr-jobs"] }),
  });
}

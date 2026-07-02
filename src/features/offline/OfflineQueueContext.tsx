import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { http } from "../../shared/api";
import { ApiError, cryptoRandomId } from "../../shared/http";
import { OfflineQueue, type QueuedOp } from "../../shared/offlineQueue";
import { queueStorage } from "../../shared/queueStorage";

interface OfflineState {
  pendingCount: number;
  /** Try to POST now; on a network failure (not an API 4xx/5xx) defer to the offline queue. */
  postOrQueue: (label: string, path: string, body?: unknown) => Promise<{ queued: boolean }>;
  flush: () => Promise<void>;
}

const OfflineCtx = createContext<OfflineState | null>(null);

export function OfflineQueueProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const [pendingCount, setPendingCount] = useState(0);

  const queueRef = useRef<OfflineQueue>(
    new OfflineQueue(queueStorage, (op: QueuedOp) =>
      http.post(op.path, op.body, { idempotencyKey: op.idempotencyKey }).then(() => undefined),
    ),
  );

  const refreshCount = useCallback(async () => {
    setPendingCount(await queueRef.current.size());
  }, []);

  const flush = useCallback(async () => {
    await queueRef.current.flush();
    await refreshCount();
  }, [refreshCount]);

  const postOrQueue = useCallback(
    async (label: string, path: string, body?: unknown) => {
      const idempotencyKey = cryptoRandomId();
      try {
        await http.post(path, body, { idempotencyKey });
        return { queued: false };
      } catch (e) {
        // API errors (validation/conflict/auth) are real failures — surface them, don't queue.
        if (e instanceof ApiError) {
          throw e;
        }
        // Network/connectivity failure → defer for later sync.
        await queueRef.current.enqueue({
          id: idempotencyKey,
          label,
          method: "POST",
          path,
          body,
          idempotencyKey,
          createdAt: Date.now(),
        });
        await refreshCount();
        return { queued: true };
      }
    },
    [refreshCount],
  );

  useEffect(() => {
    void (async () => {
      await refreshCount();
      await flush(); // attempt a drain on startup (we're likely online now)
    })();
    // Web: retry when connectivity returns. (Native uses periodic/foreground flushes.)
    const g = globalThis as { addEventListener?: (t: string, cb: () => void) => void };
    if (g.addEventListener) {
      g.addEventListener("online", () => void flush());
    }
  }, [refreshCount, flush]);

  const value = useMemo<OfflineState>(
    () => ({ pendingCount, postOrQueue, flush }),
    [pendingCount, postOrQueue, flush],
  );
  return <OfflineCtx.Provider value={value}>{children}</OfflineCtx.Provider>;
}

export function useOfflineQueue(): OfflineState {
  const ctx = useContext(OfflineCtx);
  if (!ctx) {
    throw new Error("useOfflineQueue must be used within an OfflineQueueProvider");
  }
  return ctx;
}

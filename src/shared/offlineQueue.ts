/**
 * Offline "Pending Sync" queue for deferrable mutations. When the device is offline, non-critical
 * writes (e.g. a review note, AI feedback) are queued locally and flushed in order once connectivity
 * returns. Framework-free (storage + sender are injected) so it is fully unit-testable.
 *
 * NOT for money-moving actions: per the product rules, payment/payout confirmation requires online —
 * only enqueue operations that are safe to defer and are idempotent server-side.
 */
export interface QueuedOp {
  id: string;
  label: string; // human-readable, for the pending-sync UI
  method: "POST";
  path: string; // gateway path, e.g. /api/v1/ai/suggestions/{id}/feedback
  body?: unknown;
  idempotencyKey: string; // so a replay never double-applies
  createdAt: number;
}

export interface QueueStorage {
  load(): Promise<QueuedOp[]>;
  save(ops: QueuedOp[]): Promise<void>;
}

/** Sends one queued op to the server. Should throw on failure so the op is retried later. */
export type OpSender = (op: QueuedOp) => Promise<void>;

export class OfflineQueue {
  private ops: QueuedOp[] = [];
  private loaded = false;

  constructor(
    private readonly storage: QueueStorage,
    private readonly sender: OpSender,
  ) {}

  private async ensureLoaded(): Promise<void> {
    if (!this.loaded) {
      this.ops = await this.storage.load();
      this.loaded = true;
    }
  }

  async enqueue(op: QueuedOp): Promise<void> {
    await this.ensureLoaded();
    this.ops.push(op);
    await this.storage.save(this.ops);
  }

  async pending(): Promise<QueuedOp[]> {
    await this.ensureLoaded();
    return [...this.ops];
  }

  async size(): Promise<number> {
    await this.ensureLoaded();
    return this.ops.length;
  }

  /**
   * Flush queued ops in FIFO order. Stops at the first failure (preserving order and not losing the
   * failed op) and returns how many succeeded. Safe to call repeatedly / on reconnect.
   */
  async flush(): Promise<{ sent: number; remaining: number }> {
    await this.ensureLoaded();
    let sent = 0;
    while (this.ops.length > 0) {
      const next = this.ops[0];
      try {
        await this.sender(next);
      } catch {
        break; // keep this op and the rest for the next attempt
      }
      this.ops.shift();
      sent++;
      await this.storage.save(this.ops);
    }
    return { sent, remaining: this.ops.length };
  }
}

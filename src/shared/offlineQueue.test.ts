import { OfflineQueue, type QueueStorage, type QueuedOp } from "./offlineQueue";

function memoryStorage(initial: QueuedOp[] = []): QueueStorage {
  let ops = [...initial];
  return {
    load: async () => [...ops],
    save: async (next) => void (ops = [...next]),
  };
}

function op(id: string): QueuedOp {
  return { id, label: `op ${id}`, method: "POST", path: `/x/${id}`, idempotencyKey: `k-${id}`, createdAt: 1 };
}

describe("OfflineQueue", () => {
  test("enqueues and persists in FIFO order", async () => {
    const storage = memoryStorage();
    const q = new OfflineQueue(storage, async () => {});
    await q.enqueue(op("1"));
    await q.enqueue(op("2"));
    expect((await q.pending()).map((o) => o.id)).toEqual(["1", "2"]);
    expect(await q.size()).toBe(2);
  });

  test("flush sends all when online and clears the queue", async () => {
    const sent: string[] = [];
    const q = new OfflineQueue(memoryStorage([op("1"), op("2")]), async (o) => void sent.push(o.id));
    const res = await q.flush();
    expect(sent).toEqual(["1", "2"]);
    expect(res).toEqual({ sent: 2, remaining: 0 });
    expect(await q.size()).toBe(0);
  });

  test("flush stops at first failure, preserving order for retry", async () => {
    let calls = 0;
    const q = new OfflineQueue(memoryStorage([op("1"), op("2"), op("3")]), async (o) => {
      calls++;
      if (o.id === "2") throw new Error("offline");
    });
    const res = await q.flush();
    expect(res).toEqual({ sent: 1, remaining: 2 });
    expect((await q.pending()).map((o) => o.id)).toEqual(["2", "3"]); // "1" sent, rest kept
    expect(calls).toBe(2); // tried 1 (ok) and 2 (failed); did not attempt 3
  });

  test("persisted queue survives a fresh instance (reload)", async () => {
    const storage = memoryStorage();
    const q1 = new OfflineQueue(storage, async () => {});
    await q1.enqueue(op("1"));
    const q2 = new OfflineQueue(storage, async () => {});
    expect(await q2.size()).toBe(1);
  });
});

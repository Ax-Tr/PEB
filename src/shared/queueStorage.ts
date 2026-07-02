/**
 * AsyncStorage-backed persistence for the offline queue. AsyncStorage works on native and web
 * (via its web shim), so one implementation covers all platforms. The contract lives in
 * offlineQueue.ts so the queue logic stays framework-free and unit-testable.
 */
import AsyncStorage from "@react-native-async-storage/async-storage";
import type { QueuedOp, QueueStorage } from "./offlineQueue";

const KEY = "peb.offlineQueue.v1";

export const queueStorage: QueueStorage = {
  async load() {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return [];
    try {
      return JSON.parse(raw) as QueuedOp[];
    } catch {
      return [];
    }
  },
  async save(ops) {
    await AsyncStorage.setItem(KEY, JSON.stringify(ops));
  },
};

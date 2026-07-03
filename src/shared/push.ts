/**
 * Push-notification registration. Requests permission and obtains the device push token on native;
 * on web (no Expo push token) it degrades to null. The token would be registered with the backend so
 * notification-service can target the device — that device-registration endpoint is a documented
 * backend follow-up, so for now the token is just returned/held.
 */
import { Platform } from "react-native";
import * as Notifications from "expo-notifications";
import { http } from "./api";
import { API_PREFIX } from "./constants";

export interface PushRegistration {
  token: string | null;
  status: "granted" | "denied" | "unsupported";
}

let lastToken: string | null = null;

export async function registerForPush(): Promise<PushRegistration> {
  if (Platform.OS === "web") {
    return { token: null, status: "unsupported" };
  }
  try {
    // expo-modules-core's PermissionResponse type does not resolve under this standalone tsconfig,
    // so read the fields we use via a minimal local shape.
    type PermLike = { status: string };
    let status = ((await Notifications.getPermissionsAsync()) as unknown as PermLike).status;
    if (status !== "granted") {
      status = ((await Notifications.requestPermissionsAsync()) as unknown as PermLike).status;
    }
    if (status !== "granted") {
      return { token: null, status: "denied" };
    }
    const token = (await Notifications.getExpoPushTokenAsync()) as unknown as { data: string };
    return { token: token.data, status: "granted" };
  } catch {
    // Missing native config (e.g. Expo Go without a project id) — fail soft, never crash the app.
    return { token: null, status: "unsupported" };
  }
}

/** Obtain the push token and register it with notification-service so this device can be targeted. */
export async function syncPushRegistration(): Promise<PushRegistration> {
  const reg = await registerForPush();
  if (reg.token) {
    lastToken = reg.token;
    try {
      await http.post(`${API_PREFIX}/notifications/devices`, {
        token: reg.token,
        platform: Platform.OS,
      });
    } catch {
      // Best-effort: a failed registration must never block using the app.
    }
  }
  return reg;
}

/** Unregister this device's token (best-effort) — call on logout while the session is still valid. */
export async function unregisterPush(): Promise<void> {
  if (!lastToken) {
    return;
  }
  const token = lastToken;
  lastToken = null;
  try {
    await http.del(`${API_PREFIX}/notifications/devices/${encodeURIComponent(token)}`);
  } catch {
    // ignore — token will be re-registered on next launch, and stale tokens are pruned server-side
  }
}

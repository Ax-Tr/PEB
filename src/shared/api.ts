/**
 * Composition root for the data layer: builds the unauthenticated auth poster, the {@link AuthService}
 * (token storage + refresh), and the authenticated {@link HttpClient} the whole app uses. Import
 * `api` from here; do not construct clients elsewhere.
 *
 * **Demo mode:** When {@code EXPO_PUBLIC_DEMO_MODE} is "true" (or unset and the default is enabled),
 * all API calls are served by {@link DemoHttpClient} with contract-compatible mock data. Switching
 * to the real backend requires only setting the env var to "false" — zero UI/hook changes needed.
 */
import { API_BASE_URL } from "./config";
import { AuthService } from "./auth";
import { ApiError, HttpClient } from "./http";
import { DemoHttpClient } from "./demoHttp";
import type { Problem } from "./types";
import { tokenStore } from "./tokenStore";
// tokenStore is the native/web concrete store; AuthService depends only on the TokenStore contract.

/**
 * Demo mode is ON when EXPO_PUBLIC_DEMO_MODE is "true" or when the env var is absent
 * (default to demo so the app works out-of-the-box without a backend).
 * Set EXPO_PUBLIC_DEMO_MODE=false to use the real backend.
 */
const envDemo = process.env.EXPO_PUBLIC_DEMO_MODE;
export const demoMode: boolean = envDemo === undefined || envDemo === "" || envDemo === "true";

/** Demo http client instance (created once; re-used if demo mode is on). */
const demoHttp = new DemoHttpClient();

/** Unauthenticated JSON POST for the auth bootstrap endpoints (OTP/refresh). */
async function rawPost(path: string, body: unknown): Promise<unknown> {
  // In demo mode, route auth POSTs through the demo client too.
  if (demoMode) {
    return demoHttp.post(path, body);
  }
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    let problem: Problem | undefined;
    let rawText = "";
    try {
      rawText = await res.text();
      problem = JSON.parse(rawText) as Problem;
    } catch {
      problem = undefined;
    }
    const message = problem?.detail || problem?.title || (rawText ? `Status ${res.status}: ${rawText.substring(0, 100)}` : `Status ${res.status}`);
    throw new ApiError(res.status, message, problem);
  }
  if (res.status === 204) return undefined;
  return res.json();
}

export const auth = new AuthService(tokenStore, rawPost);

/**
 * The HTTP client every hook imports. In demo mode this is the {@link DemoHttpClient};
 * in production it is the real {@link HttpClient} with bearer auth + correlation ids.
 * Both expose the same get/post/del interface — the consuming code never knows the difference.
 */
export const http: Pick<HttpClient, "get" | "post" | "del"> = demoMode
  ? demoHttp
  : new HttpClient(API_BASE_URL, auth);

export const api = { auth, http };

/**
 * Composition root for the data layer: builds the unauthenticated auth poster, the {@link AuthService}
 * (token storage + refresh), and the authenticated {@link HttpClient} the whole app uses. Import
 * `api` from here; do not construct clients elsewhere.
 */
import { API_BASE_URL } from "./config";
import { AuthService } from "./auth";
import { ApiError, HttpClient } from "./http";
import type { Problem } from "./types";
import { tokenStore } from "./tokenStore";
// tokenStore is the native/web concrete store; AuthService depends only on the TokenStore contract.

/** Unauthenticated JSON POST for the auth bootstrap endpoints (OTP/refresh). */
async function rawPost(path: string, body: unknown): Promise<unknown> {
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
export const http = new HttpClient(API_BASE_URL, auth);

export const api = { auth, http };

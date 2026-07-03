/**
 * Thin, typed HTTP client for the PEB API gateway. Adds the bearer token, correlation id, and (for
 * mutations) an idempotency key; parses RFC-7807 problem+json into a typed {@link ApiError}. Framework
 * -free (uses global fetch) so it runs on native (React Native) and web unchanged, and is unit-testable.
 */
import type { Problem } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly problem?: Problem;

  constructor(status: number, message: string, problem?: Problem) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = problem?.code;
    this.problem = problem;
  }

  /** True when the access token is missing/expired and a refresh should be attempted. */
  get isUnauthorized(): boolean {
    return this.status === 401;
  }
}

export interface TokenProvider {
  getAccessToken(): Promise<string | null>;
  /** Attempt a refresh after a 401; return the new access token or null if it failed. */
  refresh(): Promise<string | null>;
}

export interface HttpOptions {
  /** Correlation id for tracing; generated per call if omitted. */
  correlationId?: string;
  /** Idempotency key for POST/PUT money mutations. */
  idempotencyKey?: string;
  /** Set when the user has completed step-up (biometric) auth for a high-risk action. */
  stepUpVerified?: boolean;
  signal?: AbortSignal;
}

export class HttpClient {
  constructor(
    private readonly baseUrl: string,
    private readonly tokens: TokenProvider,
    private readonly fetchImpl: typeof fetch = fetch,
  ) {}

  get<T>(path: string, opts?: HttpOptions): Promise<T> {
    return this.request<T>("GET", path, undefined, opts);
  }

  post<T>(path: string, body?: unknown, opts?: HttpOptions): Promise<T> {
    return this.request<T>("POST", path, body, opts);
  }

  del<T>(path: string, opts?: HttpOptions): Promise<T> {
    return this.request<T>("DELETE", path, undefined, opts);
  }

  private async request<T>(
    method: string,
    path: string,
    body: unknown,
    opts: HttpOptions = {},
    isRetry = false,
  ): Promise<T> {
    const token = await this.tokens.getAccessToken();
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Correlation-Id": opts.correlationId ?? cryptoRandomId(),
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    if (opts.idempotencyKey && method !== "GET") {
      headers["Idempotency-Key"] = opts.idempotencyKey;
    }
    if (opts.stepUpVerified) {
      headers["X-Step-Up-Verified"] = "true";
    }

    const res = await this.fetchImpl(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: opts.signal,
    });

    if (res.status === 401 && !isRetry) {
      const refreshed = await this.tokens.refresh();
      if (refreshed) {
        return this.request<T>(method, path, body, opts, true);
      }
    }

    if (!res.ok) {
      throw await toApiError(res);
    }
    if (res.status === 204) {
      return undefined as T;
    }
    return (await res.json()) as T;
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  let problem: Problem | undefined;
  try {
    problem = (await res.json()) as Problem;
  } catch {
    problem = undefined;
  }
  const message = problem?.detail || problem?.title || `Request failed (${res.status})`;
  return new ApiError(res.status, message, problem);
}

/** RFC-4122-ish id without a native crypto dependency; fine for correlation/idempotency. */
export function cryptoRandomId(): string {
  const g = globalThis as { crypto?: { randomUUID?: () => string } };
  if (g.crypto?.randomUUID) {
    return g.crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = Math.floor(Math.random() * 16);
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

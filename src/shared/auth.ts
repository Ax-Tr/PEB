/**
 * Authentication: phone + OTP against identity-service, opaque-token storage, and silent refresh.
 * {@link AuthService} implements {@link TokenProvider} so the {@link HttpClient} can transparently
 * refresh on a 401. Dependencies are injected so the flow logic is unit-testable without a device.
 */
import { API_PREFIX } from "./constants";
import type { TokenProvider } from "./http";
import { cryptoRandomId } from "./http";
import type { TokenStore } from "./tokens";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, TENANT_ID_KEY } from "./tokens";
import type { OtpRequestResult, TokenPair } from "./types";

/** Minimal fetch surface for auth calls (unauthenticated endpoints). */
type Fetcher = (path: string, body: unknown) => Promise<unknown>;

export class AuthService implements TokenProvider {
  private accessToken: string | null = null;
  private tenantId: string | null | undefined = undefined; // undefined = not yet loaded

  constructor(
    private readonly store: TokenStore,
    private readonly post: Fetcher,
  ) {}

  async requestOtp(mobile: string): Promise<OtpRequestResult> {
    return (await this.post(`${API_PREFIX}/auth/otp/request`, { mobile })) as OtpRequestResult;
  }

  async verifyOtp(mobile: string, code: string, challengeId: string): Promise<void> {
    const tokens = (await this.post(`${API_PREFIX}/auth/otp/verify`, {
      mobile,
      otp: code,
      challengeId,
    })) as TokenPair;
    await this.persist(tokens);
  }

  async getTenantId(): Promise<string | null> {
    if (this.tenantId !== undefined) return this.tenantId;
    this.tenantId = await this.store.get(TENANT_ID_KEY);
    return this.tenantId;
  }

  async hasTenant(): Promise<boolean> {
    return (await this.getTenantId()) != null;
  }

  /** Call after the user creates their business so the app re-evaluates tenant gating. */
  setTenantId(id: string): void {
    this.tenantId = id;
    void this.store.set(TENANT_ID_KEY, id);
  }

  async getAccessToken(): Promise<string | null> {
    if (this.accessToken) return this.accessToken;
    this.accessToken = await this.store.get(ACCESS_TOKEN_KEY);
    return this.accessToken;
  }

  async refresh(): Promise<string | null> {
    const refreshToken = await this.store.get(REFRESH_TOKEN_KEY);
    if (!refreshToken) return null;
    try {
      const tokens = (await this.post(`${API_PREFIX}/auth/token/refresh`, {
        refreshToken,
      })) as TokenPair;
      await this.persist(tokens);
      return tokens.accessToken;
    } catch {
      await this.logout();
      return null;
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return (await this.getAccessToken()) != null;
  }

  async logout(): Promise<void> {
    this.accessToken = null;
    this.tenantId = undefined;
    await this.store.remove(ACCESS_TOKEN_KEY);
    await this.store.remove(REFRESH_TOKEN_KEY);
    await this.store.remove(TENANT_ID_KEY);
  }

  private async persist(tokens: TokenPair): Promise<void> {
    this.accessToken = tokens.accessToken;
    this.tenantId = tokens.tenantId ?? null;
    await this.store.set(ACCESS_TOKEN_KEY, tokens.accessToken);
    await this.store.set(REFRESH_TOKEN_KEY, tokens.refreshToken);
    if (tokens.tenantId) {
      await this.store.set(TENANT_ID_KEY, tokens.tenantId);
    } else {
      await this.store.remove(TENANT_ID_KEY);
    }
  }
}

/** A correlation id for a login attempt (kept here so the auth UI can pass one through). */
export const newAuthCorrelationId = cryptoRandomId;

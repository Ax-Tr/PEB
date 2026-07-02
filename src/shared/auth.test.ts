import { AuthService } from "./auth";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, type TokenStore } from "./tokens";

function memoryStore(): TokenStore {
  const m = new Map<string, string>();
  return {
    get: async (k) => m.get(k) ?? null,
    set: async (k, v) => void m.set(k, v),
    remove: async (k) => void m.delete(k),
  };
}

describe("AuthService", () => {
  test("verifyOtp persists tokens and authenticates", async () => {
    const store = memoryStore();
    const post = jest.fn().mockResolvedValue({
      accessToken: "acc",
      refreshToken: "ref",
      expiresInSeconds: 900,
    });
    const auth = new AuthService(store, post);

    await auth.verifyOtp("+919876543210", "123456", "chal-1");

    expect(await auth.getAccessToken()).toBe("acc");
    expect(await auth.isAuthenticated()).toBe(true);
    expect(await store.get(REFRESH_TOKEN_KEY)).toBe("ref");
  });

  test("refresh exchanges the stored refresh token for a new access token", async () => {
    const store = memoryStore();
    await store.set(REFRESH_TOKEN_KEY, "ref-1");
    const post = jest.fn().mockResolvedValue({
      accessToken: "acc-2",
      refreshToken: "ref-2",
      expiresInSeconds: 900,
    });
    const auth = new AuthService(store, post);

    const token = await auth.refresh();

    expect(token).toBe("acc-2");
    expect(post).toHaveBeenCalledWith("/api/v1/auth/token/refresh", { refreshToken: "ref-1" });
    expect(await store.get(ACCESS_TOKEN_KEY)).toBe("acc-2");
  });

  test("refresh with no stored token returns null (not authenticated)", async () => {
    const auth = new AuthService(memoryStore(), jest.fn());
    expect(await auth.refresh()).toBeNull();
  });

  test("logout clears tokens", async () => {
    const store = memoryStore();
    const post = jest.fn().mockResolvedValue({ accessToken: "a", refreshToken: "r", expiresInSeconds: 900 });
    const auth = new AuthService(store, post);
    await auth.verifyOtp("m", "c", "id");
    await auth.logout();
    expect(await auth.isAuthenticated()).toBe(false);
  });
});

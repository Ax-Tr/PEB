import { ApiError, HttpClient, type TokenProvider } from "./http";

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as unknown as Response;
}

describe("HttpClient", () => {
  const tokens: TokenProvider = {
    getAccessToken: async () => "access-1",
    refresh: async () => "access-2",
  };

  test("attaches bearer token and correlation id, returns parsed json", async () => {
    let seen: RequestInit | undefined;
    const fetchImpl = (async (_url: string, init: RequestInit) => {
      seen = init;
      return jsonResponse(200, { ok: true });
    }) as unknown as typeof fetch;

    const http = new HttpClient("http://gw", tokens, fetchImpl);
    const res = await http.get<{ ok: boolean }>("/api/v1/analytics/freshness");

    expect(res.ok).toBe(true);
    const headers = seen?.headers as Record<string, string>;
    expect(headers.Authorization).toBe("Bearer access-1");
    expect(headers["X-Correlation-Id"]).toBeTruthy();
  });

  test("parses RFC-7807 problem+json into ApiError with code", async () => {
    const fetchImpl = (async () =>
      jsonResponse(409, { title: "Conflict", detail: "Report is APPROVED", code: "CONFLICT" })) as unknown as typeof fetch;
    const http = new HttpClient("http://gw", tokens, fetchImpl);

    await expect(http.get("/x")).rejects.toMatchObject({
      status: 409,
      code: "CONFLICT",
      message: "Report is APPROVED",
    });
  });

  test("refreshes once on 401 then retries", async () => {
    // Stateful provider: refresh() rotates the token, as AuthService does in production.
    let current = "access-1";
    const stateful: TokenProvider = {
      getAccessToken: async () => current,
      refresh: async () => {
        current = "access-2";
        return current;
      },
    };
    const calls: string[] = [];
    const fetchImpl = (async (_url: string, init: RequestInit) => {
      const auth = (init.headers as Record<string, string>).Authorization;
      calls.push(auth);
      if (auth === "Bearer access-1") return jsonResponse(401, { title: "Unauthorized" });
      return jsonResponse(200, { ok: true });
    }) as unknown as typeof fetch;

    const http = new HttpClient("http://gw", stateful, fetchImpl);
    const res = await http.get<{ ok: boolean }>("/secure");

    expect(res.ok).toBe(true);
    expect(calls).toEqual(["Bearer access-1", "Bearer access-2"]); // retried with refreshed token
  });

  test("does not retry indefinitely when refresh fails", async () => {
    const failing: TokenProvider = { getAccessToken: async () => "t", refresh: async () => null };
    const fetchImpl = (async () => jsonResponse(401, { title: "Unauthorized" })) as unknown as typeof fetch;
    const http = new HttpClient("http://gw", failing, fetchImpl);
    await expect(http.get("/secure")).rejects.toBeInstanceOf(ApiError);
  });
});

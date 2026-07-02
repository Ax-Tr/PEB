/** Token-store contract + keys. Framework-free so auth logic and its tests avoid a native dependency. */
export interface TokenStore {
  get(key: string): Promise<string | null>;
  set(key: string, value: string): Promise<void>;
  remove(key: string): Promise<void>;
}

export const ACCESS_TOKEN_KEY = "peb.accessToken";
export const REFRESH_TOKEN_KEY = "peb.refreshToken";

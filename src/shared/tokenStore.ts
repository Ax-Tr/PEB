/**
 * Concrete secure token storage. On native, tokens live in the OS keystore (expo-secure-store); on
 * web they fall back to localStorage (a browser has no keystore — pair with short token TTLs +
 * refresh). Never store tokens in plain AsyncStorage on native. The contract/keys live in tokens.ts
 * so the framework-free modules (auth logic) don't pull in react-native.
 */
import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";
import type { TokenStore } from "./tokens";

const webStore: TokenStore = {
  async get(key) {
    if (typeof localStorage === "undefined") return null;
    return localStorage.getItem(key);
  },
  async set(key, value) {
    if (typeof localStorage !== "undefined") localStorage.setItem(key, value);
  },
  async remove(key) {
    if (typeof localStorage !== "undefined") localStorage.removeItem(key);
  },
};

const nativeStore: TokenStore = {
  get: (key) => SecureStore.getItemAsync(key),
  set: (key, value) => SecureStore.setItemAsync(key, value),
  remove: (key) => SecureStore.deleteItemAsync(key),
};

export const tokenStore: TokenStore = Platform.OS === "web" ? webStore : nativeStore;

/**
 * Runtime configuration. The API base URL is resolved per platform: on web it defaults to the
 * same origin (the gateway is served/proxied there); on native it uses a configured host. Override
 * via the EXPO_PUBLIC_API_BASE_URL env at build time.
 */
import { Platform } from "react-native";

function resolveBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (fromEnv && fromEnv.length > 0) {
    return fromEnv.replace(/\/$/, "");
  }
  if (Platform.OS === "web" && typeof window !== "undefined") {
    if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
      return "http://localhost:8080";
    }
    return window.location.origin;
  }
  // Native dev default (Android emulator maps host loopback to 10.0.2.2; adjust per environment).
  return Platform.OS === "android" ? "http://10.0.2.2:8080" : "http://localhost:8080";
}

export const API_BASE_URL = resolveBaseUrl();
export { API_PREFIX } from "./constants";

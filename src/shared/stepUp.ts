/**
 * Step-up (re-)authentication for high-risk actions (e.g. a large payout). On native this is a
 * biometric/device-credential prompt (expo-local-authentication); on web there is no platform
 * biometric, so it degrades to an explicit confirmation (documented as weaker). The result is passed
 * to the API as the {@code X-Step-Up-Verified} header, which the backend requires for high-risk flows.
 */
import { Platform } from "react-native";
import * as LocalAuthentication from "expo-local-authentication";

/** Amounts at/above this (paise) are treated as high-value and prompt step-up. ₹50,000. */
export const STEP_UP_THRESHOLD_MINOR = 5_000_000;

export async function requestStepUp(reason: string): Promise<boolean> {
  if (Platform.OS === "web") {
    const g = globalThis as { confirm?: (m: string) => boolean };
    return g.confirm ? g.confirm(`${reason}\n\nConfirm to proceed.`) : false;
  }
  const hasHardware = await LocalAuthentication.hasHardwareAsync();
  const enrolled = await LocalAuthentication.isEnrolledAsync();
  if (!hasHardware || !enrolled) {
    return false;
  }
  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: reason,
    disableDeviceFallback: false,
  });
  return result.success;
}

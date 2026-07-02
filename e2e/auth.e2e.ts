/**
 * e2e: phone + OTP login → lands on the dashboard. Runs against a build pointed at a test backend
 * (or a mocked gateway) where a known OTP is accepted. Targets stable testIDs, not visible text, so
 * it is i18n-proof.
 */
import { by, device, element, expect } from "detox";

describe("Authentication", () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: true });
  });

  it("logs in with phone + OTP and shows the dashboard", async () => {
    await element(by.id("login-mobile")).typeText("9876543210");
    await element(by.id("login-send-otp")).tap();

    await expect(element(by.id("login-otp"))).toBeVisible();
    await element(by.id("login-otp")).typeText("123456");
    await element(by.id("login-verify")).tap();

    await expect(element(by.id("dashboard-title"))).toBeVisible();
  });
});

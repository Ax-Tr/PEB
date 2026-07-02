/**
 * e2e: create a payment-collection request. Assumes an authenticated session (the auth spec runs
 * first, or the app is launched with a seeded token). Navigates to the Receive tab and generates a
 * request. Tab is reached by its label; field/button by stable testIDs.
 */
import { by, device, element, expect } from "detox";

describe("Receive payment", () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: false });
  });

  it("generates a collection request from an amount", async () => {
    await element(by.text("Receive")).tap();
    await element(by.id("receive-amount")).typeText("1499");
    await element(by.id("receive-generate")).tap();
    // On success the screen shows the "show this to your customer" card with the amount.
    await expect(element(by.text("Show this to your customer"))).toBeVisible();
  });
});

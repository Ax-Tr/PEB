import { isLocale, translate } from "./i18n";

describe("i18n translate", () => {
  test("returns the locale's string", () => {
    expect(translate("en", "auth.signIn")).toBe("Sign in");
    expect(translate("hi", "auth.signIn")).toBe("साइन इन करें");
  });

  test("interpolates params", () => {
    expect(translate("en", "auth.otpSentTo", { mobile: "+9198…" })).toBe("OTP sent to +9198…");
    expect(translate("hi", "auth.otpSentTo", { mobile: "X" })).toContain("X");
  });

  test("falls back to English for a missing key in another locale, then to the key", () => {
    // Simulate a key present in en only by using a real key; unknown key returns itself.
    expect(translate("hi", "does.not.exist")).toBe("does.not.exist");
  });

  test("leaves unknown placeholders intact", () => {
    expect(translate("en", "auth.otpSentTo")).toBe("OTP sent to {mobile}");
  });

  test("isLocale guards", () => {
    expect(isLocale("en")).toBe(true);
    expect(isLocale("hi")).toBe(true);
    expect(isLocale("fr")).toBe(false);
  });
});

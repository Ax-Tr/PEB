/**
 * Minimal, dependency-free i18n. `translate()` is pure (unit-tested); the React layer (I18nProvider /
 * useI18n) adds locale state + switching. English is the base; missing keys fall back to English, then
 * to the key itself. Params interpolate with {name} placeholders. Hindi is included as a second
 * locale given the Indian-MSME audience; more can be added by extending the dictionaries.
 */
export const LOCALES = ["en", "hi"] as const;
export type Locale = (typeof LOCALES)[number];

type Dict = Record<string, string>;

const en: Dict = {
  "app.tagline": "Your business finances, simplified.",
  "auth.signIn": "Sign in",
  "auth.mobile": "Mobile number",
  "auth.sendOtp": "Send OTP",
  "auth.enterOtp": "Enter OTP",
  "auth.otpSentTo": "OTP sent to {mobile}",
  "auth.verify": "Verify & continue",
  "auth.changeNumber": "Change number",
  "common.signOut": "Sign out",
  "common.retry": "Retry",
  "tab.home": "Home",
  "tab.receive": "Receive",
  "tab.pay": "Pay",
  "tab.books": "Books",
  "tab.more": "More",
  "more.title": "More",
  "more.language": "Language",
};

const hi: Dict = {
  "app.tagline": "आपके व्यवसाय का वित्त, आसान।",
  "auth.signIn": "साइन इन करें",
  "auth.mobile": "मोबाइल नंबर",
  "auth.sendOtp": "OTP भेजें",
  "auth.enterOtp": "OTP दर्ज करें",
  "auth.otpSentTo": "{mobile} पर OTP भेजा गया",
  "auth.verify": "सत्यापित करें और जारी रखें",
  "auth.changeNumber": "नंबर बदलें",
  "common.signOut": "साइन आउट",
  "common.retry": "पुनः प्रयास करें",
  "tab.home": "होम",
  "tab.receive": "प्राप्त करें",
  "tab.pay": "भुगतान",
  "tab.books": "बही-खाता",
  "tab.more": "अधिक",
  "more.title": "अधिक",
  "more.language": "भाषा",
};

const DICTS: Record<Locale, Dict> = { en, hi };

export function isLocale(value: string): value is Locale {
  return (LOCALES as readonly string[]).includes(value);
}

export function translate(locale: Locale, key: string, params?: Record<string, string | number>): string {
  const template = DICTS[locale]?.[key] ?? DICTS.en[key] ?? key;
  return interpolate(template, params);
}

function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, name: string) =>
    name in params ? String(params[name]) : match,
  );
}

/** Best-effort initial locale from the platform; defaults to English. */
export function resolveInitialLocale(): Locale {
  const nav = (globalThis as { navigator?: { language?: string } }).navigator;
  const lang = nav?.language?.slice(0, 2).toLowerCase();
  return lang && isLocale(lang) ? lang : "en";
}

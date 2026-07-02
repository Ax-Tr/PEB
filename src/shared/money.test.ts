import { formatINR, parseRupeesToMinor, toRupees } from "./money";

describe("money (integer paise)", () => {
  test("formats paise as Indian-locale rupees", () => {
    expect(formatINR(0)).toBe("₹0.00");
    expect(formatINR(123450)).toBe("₹1,234.50");
    expect(formatINR(5)).toBe("₹0.05");
    expect(formatINR(-99900)).toBe("-₹999.00");
  });

  test("uses Indian digit grouping (lakh/crore)", () => {
    expect(formatINR(1234567800)).toBe("₹1,23,45,678.00");
  });

  test("parses rupee strings to paise", () => {
    expect(parseRupeesToMinor("1499")).toBe(149900);
    expect(parseRupeesToMinor("1,234.50")).toBe(123450);
    expect(parseRupeesToMinor("₹ 10.5")).toBe(1050);
    expect(parseRupeesToMinor("-999")).toBe(-99900);
  });

  test("round-trips format <-> parse", () => {
    for (const minor of [0, 5, 100, 123450, 999999]) {
      expect(parseRupeesToMinor(formatINR(minor).replace("₹", ""))).toBe(minor);
    }
  });

  test("rejects invalid amounts", () => {
    expect(() => parseRupeesToMinor("abc")).toThrow();
    expect(() => parseRupeesToMinor("1.234")).toThrow(); // > 2 decimals
  });

  test("toRupees converts without extra precision loss", () => {
    expect(toRupees(123450)).toBe(1234.5);
  });
});

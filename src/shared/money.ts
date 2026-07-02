/**
 * Money is always integer paise (₹1 = 100 paise) — never floating point — mirroring the backend
 * `Money` value object. These helpers convert only at the presentation boundary.
 */

export type Minor = number; // integer paise

/** Format paise as Indian-locale rupees, e.g. 123450 -> "₹1,234.50". */
export function formatINR(minor: Minor): string {
  const negative = minor < 0;
  const abs = Math.abs(Math.trunc(minor));
  const rupees = Math.floor(abs / 100);
  const paise = abs % 100;
  const grouped = groupIndian(rupees);
  const sign = negative ? "-" : "";
  return `${sign}₹${grouped}.${paise.toString().padStart(2, "0")}`;
}

/** Parse a rupee string/number (e.g. "1,234.50") to integer paise. Throws on invalid input. */
export function parseRupeesToMinor(value: string | number): Minor {
  const raw = typeof value === "number" ? value.toString() : value.replace(/[,\s₹]/g, "");
  if (!/^-?\d+(\.\d{1,2})?$/.test(raw)) {
    throw new Error(`Invalid rupee amount: "${value}"`);
  }
  const [rupees, paise = ""] = raw.replace("-", "").split(".");
  const minor = Number(rupees) * 100 + Number(paise.padEnd(2, "0"));
  return raw.startsWith("-") ? -minor : minor;
}

/** Rupees as a plain number for inputs/charts (loses no precision within safe integer range). */
export function toRupees(minor: Minor): number {
  return minor / 100;
}

/** Indian digit grouping (lakh/crore): 1234567 -> "12,34,567". */
function groupIndian(n: number): string {
  const s = n.toString();
  if (s.length <= 3) {
    return s;
  }
  const last3 = s.slice(-3);
  const rest = s.slice(0, -3);
  return rest.replace(/\B(?=(\d{2})+(?!\d))/g, ",") + "," + last3;
}

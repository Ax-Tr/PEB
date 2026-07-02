import React from "react";
import { StyleSheet, Text } from "react-native";
import { formatINR, type Minor } from "../shared/money";
import { theme } from "../theme/theme";

interface MoneyProps {
  minor: Minor;
  size?: "body" | "title" | "h2" | "h1";
  /** Colour negatives red and positives green (for P&L/cashflow). */
  signed?: boolean;
}

/** Renders integer paise as Indian-locale rupees. Never does float math. */
export function Money({ minor, size = "title", signed = false }: MoneyProps): React.ReactElement {
  const color = signed
    ? minor < 0
      ? theme.color.danger
      : theme.color.success
    : theme.color.text;
  return <Text style={[styles.base, { fontSize: theme.font[size], color }]}>{formatINR(minor)}</Text>;
}

const styles = StyleSheet.create({
  base: { fontWeight: "700" },
});

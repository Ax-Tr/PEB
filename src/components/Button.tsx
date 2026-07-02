import React from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text } from "react-native";
import { theme } from "../theme/theme";

interface ButtonProps {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: "primary" | "secondary";
  testID?: string;
}

/** Primary action button with a busy state and a 44px min tap target (a11y). */
export function Button({
  title,
  onPress,
  loading = false,
  disabled = false,
  variant = "primary",
  testID,
}: ButtonProps): React.ReactElement {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        variant === "primary" ? styles.primary : styles.secondary,
        (pressed || isDisabled) && styles.dim,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={theme.color.primaryText} />
      ) : (
        <Text style={styles.label}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 48,
    borderRadius: theme.radius.md,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: theme.space(4),
  },
  primary: { backgroundColor: theme.color.primary },
  secondary: { backgroundColor: theme.color.surfaceAlt, borderWidth: 1, borderColor: theme.color.border },
  dim: { opacity: 0.6 },
  label: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "600" },
});

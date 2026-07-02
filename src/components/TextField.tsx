import React from "react";
import { StyleSheet, Text, TextInput, View } from "react-native";
import { theme } from "../theme/theme";

interface TextFieldProps {
  label: string;
  value: string;
  onChangeText: (t: string) => void;
  placeholder?: string;
  keyboardType?: "default" | "number-pad" | "phone-pad" | "decimal-pad";
  maxLength?: number;
  error?: string;
  autoFocus?: boolean;
  testID?: string;
}

export function TextField({
  label,
  value,
  onChangeText,
  placeholder,
  keyboardType = "default",
  maxLength,
  error,
  autoFocus,
  testID,
}: TextFieldProps): React.ReactElement {
  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        testID={testID}
        accessibilityLabel={label}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={theme.color.textMuted}
        keyboardType={keyboardType}
        maxLength={maxLength}
        autoFocus={autoFocus}
        style={[styles.input, error ? styles.inputError : null]}
      />
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: theme.space(1) },
  label: { color: theme.color.textMuted, fontSize: theme.font.caption },
  input: {
    minHeight: 48,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.color.border,
    backgroundColor: theme.color.surface,
    color: theme.color.text,
    paddingHorizontal: theme.space(3),
    fontSize: theme.font.body,
  },
  inputError: { borderColor: theme.color.danger },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

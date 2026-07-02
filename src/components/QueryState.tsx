import React from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import type { UseQueryResult } from "@tanstack/react-query";
import { ApiError } from "../shared/http";
import { theme } from "../theme/theme";
import { Button } from "./Button";

interface QueryStateProps<T> {
  query: UseQueryResult<T>;
  children: (data: T) => React.ReactElement;
  emptyWhen?: (data: T) => boolean;
  emptyText?: string;
}

/**
 * Renders the four canonical states for a server query — loading / error / empty / success — so every
 * screen handles them consistently (coding-standards requirement). `error` shows the RFC-7807 detail.
 */
export function QueryState<T>({
  query,
  children,
  emptyWhen,
  emptyText = "Nothing here yet.",
}: QueryStateProps<T>): React.ReactElement {
  if (query.isLoading) {
    return (
      <View style={styles.center} accessibilityLabel="Loading">
        <ActivityIndicator color={theme.color.primary} />
      </View>
    );
  }
  if (query.isError) {
    const message =
      query.error instanceof ApiError ? query.error.message : "Something went wrong. Please try again.";
    return (
      <View style={styles.center}>
        <Text style={styles.errorTitle}>Couldn’t load this</Text>
        <Text style={styles.errorMsg}>{message}</Text>
        <Button title="Retry" variant="secondary" onPress={() => void query.refetch()} />
      </View>
    );
  }
  const data = query.data as T;
  if (emptyWhen && emptyWhen(data)) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorMsg}>{emptyText}</Text>
      </View>
    );
  }
  return children(data);
}

const styles = StyleSheet.create({
  center: { alignItems: "center", justifyContent: "center", padding: theme.space(6), gap: theme.space(3) },
  errorTitle: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  errorMsg: { color: theme.color.textMuted, fontSize: theme.font.body, textAlign: "center" },
});

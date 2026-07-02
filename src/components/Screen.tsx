import React from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { theme } from "../theme/theme";

interface ScreenProps {
  children: React.ReactNode;
  scroll?: boolean;
}

/** Standard screen wrapper: safe area, background, padding, optional scroll. */
export function Screen({ children, scroll = true }: ScreenProps): React.ReactElement {
  const inner = <View style={styles.inner}>{children}</View>;
  return (
    <SafeAreaView style={styles.safe}>
      {scroll ? (
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          {inner}
        </ScrollView>
      ) : (
        inner
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: theme.color.bg },
  scroll: { flexGrow: 1 },
  inner: { padding: theme.space(4), gap: theme.space(3) },
});

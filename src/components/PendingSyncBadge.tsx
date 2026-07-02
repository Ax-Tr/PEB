import React from "react";
import { Pressable, StyleSheet, Text } from "react-native";
import { useOfflineQueue } from "../features/offline/OfflineQueueContext";
import { theme } from "../theme/theme";

/** Header indicator: shows how many mutations are waiting to sync; tap to retry now. Hidden at zero. */
export function PendingSyncBadge(): React.ReactElement | null {
  const { pendingCount, flush } = useOfflineQueue();
  if (pendingCount === 0) {
    return null;
  }
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${pendingCount} pending sync. Tap to retry.`}
      onPress={() => void flush()}
      style={styles.badge}
    >
      <Text style={styles.text}>⟳ {pendingCount} pending</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  badge: {
    backgroundColor: theme.color.surfaceAlt,
    borderColor: theme.color.warning,
    borderWidth: 1,
    borderRadius: theme.radius.pill,
    paddingHorizontal: theme.space(3),
    paddingVertical: theme.space(1),
    marginRight: theme.space(3),
  },
  text: { color: theme.color.warning, fontSize: theme.font.caption, fontWeight: "700" },
});

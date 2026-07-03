import React, { useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { theme } from "../theme/theme";

interface EntityPickerProps<T> {
  label: string;
  items: T[] | undefined;
  loading?: boolean;
  selectedId: string | null;
  getId: (item: T) => string;
  getLabel: (item: T) => string;
  onSelect: (item: T) => void;
  emptyText?: string;
  testID?: string;
}

/**
 * Inline single-select picker over a list (no modal dependency). Shows the current selection; tapping
 * expands a scrollable list of options. Used to pick a real customer/vendor instead of typing an id.
 */
export function EntityPicker<T>({
  label,
  items,
  loading = false,
  selectedId,
  getId,
  getLabel,
  onSelect,
  emptyText = "None available",
  testID,
}: EntityPickerProps<T>): React.ReactElement {
  const [open, setOpen] = useState(false);
  const selected = items?.find((i) => getId(i) === selectedId);

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>{label}</Text>
      <Pressable
        testID={testID}
        accessibilityRole="button"
        accessibilityLabel={`${label}: ${selected ? getLabel(selected) : "choose"}`}
        onPress={() => setOpen((o) => !o)}
        style={styles.select}
      >
        <Text style={[styles.selectText, !selected && styles.placeholder]}>
          {loading ? "Loading…" : selected ? getLabel(selected) : "Tap to choose"}
        </Text>
        <Text style={styles.chevron}>{open ? "▲" : "▼"}</Text>
      </Pressable>
      {open ? (
        <View style={styles.list}>
          {loading ? (
            <ActivityIndicator color={theme.color.primary} />
          ) : items && items.length > 0 ? (
            items.map((item) => {
              const id = getId(item);
              return (
                <Pressable
                  key={id}
                  accessibilityRole="button"
                  onPress={() => {
                    onSelect(item);
                    setOpen(false);
                  }}
                  style={[styles.option, id === selectedId && styles.optionOn]}
                >
                  <Text style={styles.optionText}>{getLabel(item)}</Text>
                </Pressable>
              );
            })
          ) : (
            <Text style={styles.empty}>{emptyText}</Text>
          )}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: theme.space(1) },
  label: { color: theme.color.textMuted, fontSize: theme.font.caption },
  select: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: theme.color.border,
    borderRadius: theme.radius.md,
    backgroundColor: theme.color.surface,
    paddingHorizontal: theme.space(3),
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  selectText: { color: theme.color.text, fontSize: theme.font.body },
  placeholder: { color: theme.color.textMuted },
  chevron: { color: theme.color.textMuted },
  list: { borderWidth: 1, borderColor: theme.color.border, borderRadius: theme.radius.md, overflow: "hidden" },
  option: { padding: theme.space(3), borderBottomWidth: 1, borderBottomColor: theme.color.border },
  optionOn: { backgroundColor: theme.color.surfaceAlt },
  optionText: { color: theme.color.text, fontSize: theme.font.body },
  empty: { color: theme.color.textMuted, fontSize: theme.font.caption, padding: theme.space(3) },
});

import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Screen } from "../../components/Screen";
import { theme } from "../../theme/theme";
import { LOCALES } from "../../shared/i18n";
import { useI18n } from "../i18n/I18nContext";
import type { MoreStackParamList } from "../../navigation/types";

type Props = StackScreenProps<MoreStackParamList, "MoreMenu">;

const ITEMS: { route: "Insights" | "DataRights" | "Assistant"; title: string; desc: string }[] = [
  { route: "Insights", title: "Insights", desc: "Aging, cashflow by month, product profitability" },
  { route: "Assistant", title: "AI assistant", desc: "Ask questions · review suggestions · anomalies" },
  { route: "DataRights", title: "Data rights (DPDP)", desc: "Access / erasure / grievance requests" },
];

export function MoreMenuScreen({ navigation }: Props): React.ReactElement {
  const { locale, setLocale, t } = useI18n();
  return (
    <Screen>
      <Text style={styles.title}>{t("more.title")}</Text>
      <View style={styles.list}>
        {ITEMS.map((it) => (
          <Pressable
            key={it.route}
            accessibilityRole="button"
            accessibilityLabel={it.title}
            onPress={() => navigation.navigate(it.route)}
            style={styles.item}
          >
            <Text style={styles.itemTitle}>{it.title}</Text>
            <Text style={styles.itemDesc}>{it.desc}</Text>
          </Pressable>
        ))}
      </View>

      <Text style={styles.sectionLabel}>{t("more.language")}</Text>
      <View style={styles.langRow}>
        {LOCALES.map((l) => (
          <Pressable
            key={l}
            accessibilityRole="button"
            accessibilityLabel={`Language ${l}`}
            onPress={() => setLocale(l)}
            style={[styles.lang, l === locale && styles.langOn]}
          >
            <Text style={[styles.langText, l === locale && styles.langTextOn]}>{l.toUpperCase()}</Text>
          </Pressable>
        ))}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  list: { gap: theme.space(3) },
  item: {
    backgroundColor: theme.color.surface,
    borderRadius: theme.radius.lg,
    borderWidth: 1,
    borderColor: theme.color.border,
    padding: theme.space(4),
    gap: theme.space(1),
  },
  itemTitle: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  itemDesc: { color: theme.color.textMuted, fontSize: theme.font.caption },
  sectionLabel: { color: theme.color.textMuted, fontSize: theme.font.caption, textTransform: "uppercase", letterSpacing: 1, marginTop: theme.space(2) },
  langRow: { flexDirection: "row", gap: theme.space(2) },
  lang: { borderWidth: 1, borderColor: theme.color.border, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(4), paddingVertical: theme.space(2) },
  langOn: { backgroundColor: theme.color.primary, borderColor: theme.color.primary },
  langText: { color: theme.color.textMuted, fontWeight: "700" },
  langTextOn: { color: theme.color.primaryText },
});

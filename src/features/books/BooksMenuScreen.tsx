import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Screen } from "../../components/Screen";
import { theme } from "../../theme/theme";
import type { BooksStackParamList } from "../../navigation/types";

type Props = StackScreenProps<BooksStackParamList, "BooksMenu">;

const ITEMS: { route: keyof BooksStackParamList; title: string; desc: string }[] = [
  { route: "InvoiceList", title: "Invoices", desc: "View invoices; create and send a GST invoice" },
  { route: "PayoutList", title: "Payouts", desc: "View vendor/employee payouts and their status" },
  { route: "CommitmentList", title: "Commitments", desc: "Track promises, due dates, and broken follow-ups" },
  { route: "InstallmentList", title: "Installments", desc: "Receivable and payable EMI schedules" },
  { route: "ReminderList", title: "Reminders", desc: "Schedule and review follow-up reminders" },
  { route: "BankOcrCapture", title: "OCR bank capture", desc: "Extract and review vendor bank details" },
  { route: "Reconciliation", title: "Reconciliation", desc: "Match bank vs books; confirm/reject" },
  { route: "Compliance", title: "Compliance reports", desc: "GST/TDS reports · review · approve · file" },
];

export function BooksMenuScreen({ navigation }: Props): React.ReactElement {
  return (
    <Screen>
      <Text style={styles.title}>Books</Text>
      <View style={styles.list}>
        {ITEMS.map((it) => (
          <Pressable
            key={it.route}
            accessibilityRole="button"
            accessibilityLabel={it.title}
            onPress={() => navigation.navigate(it.route as never)}
            style={styles.item}
          >
            <Text style={styles.itemTitle}>{it.title}</Text>
            <Text style={styles.itemDesc}>{it.desc}</Text>
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
});

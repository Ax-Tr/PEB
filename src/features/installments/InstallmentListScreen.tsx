import React, { useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { BooksStackParamList } from "../../navigation/types";
import type { Installment } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useInstallments } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "InstallmentList">;

/** Lists receivable/payable installment schedules with outstanding balances. */
export function InstallmentListScreen({ navigation }: Props): React.ReactElement {
  const [type, setType] = useState<"RECEIVABLE" | "PAYABLE">("RECEIVABLE");
  const installments = useInstallments(type);
  const total = useMemo(
    () => installments.data?.reduce((sum, item) => sum + item.outstandingMinor, 0) ?? 0,
    [installments.data],
  );

  return (
    <Screen>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Installments</Text>
          <Text style={styles.sub}>
            {type === "RECEIVABLE" ? "To collect" : "To pay"}: <Money minor={total} size="body" />
          </Text>
        </View>
        <Button title="+ New" onPress={() => navigation.navigate("InstallmentCreate", { type })} />
      </View>

      <View style={styles.filters}>
        {(["RECEIVABLE", "PAYABLE"] as const).map((t) => (
          <Button key={t} title={t} variant={type === t ? "primary" : "secondary"} onPress={() => setType(t)} />
        ))}
      </View>

      <QueryState query={installments} emptyWhen={(l) => l.length === 0} emptyText="No installment schedules yet.">
        {(list: Installment[]) => (
          <View style={styles.list}>
            {list.map((item) => (
              <Pressable
                key={item.id}
                accessibilityRole="button"
                accessibilityLabel={`Open installment ${item.counterpartyName ?? item.type}`}
                onPress={() =>
                  navigation.navigate("InstallmentDetail", {
                    installmentId: item.id,
                    title: item.counterpartyName ?? "Installment",
                  })
                }
              >
                <Card>
                  <View style={styles.row}>
                    <Text style={styles.name}>{item.counterpartyName ?? item.type}</Text>
                    <Text style={styles.status}>{item.status}</Text>
                  </View>
                  <View style={styles.row}>
                    <Text style={styles.sub}>
                      {item.numberOfEmis} EMI(s) · {item.frequency}
                    </Text>
                    <Money minor={item.outstandingMinor} size="title" />
                  </View>
                </Card>
              </Pressable>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  filters: { flexDirection: "row", gap: theme.space(2), flexWrap: "wrap" },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  name: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700", flex: 1 },
  status: { color: theme.color.primary, fontSize: theme.font.caption, fontWeight: "800" },
});

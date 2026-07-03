import React, { useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { BooksStackParamList } from "../../navigation/types";
import type { Commitment } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useCommitments } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "CommitmentList">;

const FILTERS = ["ALL", "PROMISED", "PARTIALLY_PAID", "BROKEN"] as const;

/** Owner-facing payment promise list: due dates, outstanding amounts, and broken promises. */
export function CommitmentListScreen({ navigation }: Props): React.ReactElement {
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("ALL");
  const status = filter === "ALL" ? undefined : filter;
  const commitments = useCommitments(status);
  const totalOutstanding = useMemo(
    () => commitments.data?.reduce((sum, c) => sum + c.outstandingMinor, 0) ?? 0,
    [commitments.data],
  );

  return (
    <Screen>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Commitments</Text>
          <Text style={styles.sub}>Outstanding promises: <MoneyText minor={totalOutstanding} /></Text>
        </View>
        <Button title="+ New" onPress={() => navigation.navigate("CommitmentCreate")} />
      </View>

      <View style={styles.filters}>
        {FILTERS.map((f) => (
          <Pressable key={f} onPress={() => setFilter(f)} style={[styles.chip, filter === f && styles.chipActive]}>
            <Text style={[styles.chipText, filter === f && styles.chipTextActive]}>{f.replace("_", " ")}</Text>
          </Pressable>
        ))}
      </View>

      <QueryState query={commitments} emptyWhen={(l) => l.length === 0} emptyText="No commitments yet.">
        {(list: Commitment[]) => (
          <View style={styles.list}>
            {list.map((c) => (
              <Pressable
                key={c.id}
                accessibilityRole="button"
                accessibilityLabel={`Open commitment ${c.counterpartyName ?? c.counterpartyType}`}
                onPress={() =>
                  navigation.navigate("CommitmentDetail", {
                    commitmentId: c.id,
                    title: c.counterpartyName ?? "Commitment",
                  })
                }
              >
                <Card>
                  <View style={styles.row}>
                    <Text style={styles.name}>{c.counterpartyName ?? c.counterpartyType}</Text>
                    <StatusBadge status={c.status} />
                  </View>
                  <View style={styles.row}>
                    <Text style={styles.sub}>Due {formatDate(c.dueDate)}</Text>
                    <Money minor={c.outstandingMinor} size="title" />
                  </View>
                  {c.description ? <Text style={styles.sub}>{c.description}</Text> : null}
                </Card>
              </Pressable>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

function StatusBadge({ status }: { status: string }): React.ReactElement {
  const color = status === "BROKEN" ? theme.color.danger : status === "PAID" ? theme.color.success : theme.color.warning;
  return (
    <View style={[styles.badge, { borderColor: color }]}>
      <Text style={[styles.badgeText, { color }]}>{status.replace("_", " ")}</Text>
    </View>
  );
}

function MoneyText({ minor }: { minor: number }): React.ReactElement {
  return <Money minor={minor} size="body" />;
}

function formatDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString();
}

const styles = StyleSheet.create({
  header: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  filters: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  chip: {
    borderWidth: 1,
    borderColor: theme.color.border,
    borderRadius: theme.radius.pill,
    paddingHorizontal: theme.space(3),
    paddingVertical: theme.space(2),
  },
  chipActive: { backgroundColor: theme.color.primary, borderColor: theme.color.primary },
  chipText: { color: theme.color.textMuted, fontSize: theme.font.caption, fontWeight: "700" },
  chipTextActive: { color: theme.color.text },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  name: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700", flex: 1 },
  badge: { borderWidth: 1, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(2), paddingVertical: theme.space(1) },
  badgeText: { fontSize: theme.font.caption, fontWeight: "700" },
});

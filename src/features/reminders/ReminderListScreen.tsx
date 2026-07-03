import React from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { BooksStackParamList } from "../../navigation/types";
import type { Reminder } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useReminders } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "ReminderList">;

/** Tenant-wide reminder schedule list with delivery status visibility. */
export function ReminderListScreen({ navigation }: Props): React.ReactElement {
  const reminders = useReminders();

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Reminders</Text>
        <Button title="+ New" onPress={() => navigation.navigate("ReminderCreate", undefined)} />
      </View>

      <QueryState query={reminders} emptyWhen={(l) => l.length === 0} emptyText="No reminders scheduled.">
        {(list: Reminder[]) => (
          <View style={styles.list}>
            {list.map((r) => (
              <Card key={r.id}>
                <View style={styles.row}>
                  <Text style={styles.name}>{r.templateCode}</Text>
                  <Text style={styles.status}>{r.status}</Text>
                </View>
                <Text style={styles.sub}>
                  {r.channel} to {maskRecipient(r.recipient)}
                </Text>
                <Text style={styles.sub}>
                  Send {formatDate(r.sendOn)} · due {formatDate(r.dueDate)} · D-{r.offsetDays}
                </Text>
                {r.sourceRef ? <Text style={styles.sub}>Source: {r.sourceType ?? "SOURCE"} {r.sourceRef}</Text> : null}
              </Card>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

function maskRecipient(value: string): string {
  if (value.includes("@")) {
    const [name, domain] = value.split("@");
    return `${name.slice(0, 2)}***@${domain}`;
  }
  return value.length > 4 ? `******${value.slice(-4)}` : value;
}

function formatDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString();
}

const styles = StyleSheet.create({
  header: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", justifyContent: "space-between", gap: theme.space(3), alignItems: "center" },
  name: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700", flex: 1 },
  status: { color: theme.color.primary, fontSize: theme.font.caption, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
});

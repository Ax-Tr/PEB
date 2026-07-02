import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { MatchResponse } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useConfirmMatch, useRejectMatch, useRunReconciliation, useSuggestions } from "./hooks";

/** Review suggested matches from the reconciliation engine and confirm/reject them (audited). */
export function ReconciliationScreen(): React.ReactElement {
  const suggestions = useSuggestions();
  const run = useRunReconciliation();
  const confirm = useConfirmMatch();
  const reject = useRejectMatch();

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Reconciliation</Text>
        <Button title="Run" onPress={() => run.mutate()} loading={run.isPending} />
      </View>
      {run.data ? (
        <Text style={styles.sub}>
          Auto-matched {run.data.autoMatched} · Suggested {run.data.suggested} · Exceptions{" "}
          {run.data.exceptionsCreated}
        </Text>
      ) : null}

      <QueryState
        query={suggestions}
        emptyWhen={(list) => list.length === 0}
        emptyText="No suggested matches. Run reconciliation to find some."
      >
        {(list: MatchResponse[]) => (
          <View style={styles.list}>
            {list.map((m) => (
              <Card key={m.id}>
                <Text style={styles.matchTitle}>Suggested match</Text>
                <Text style={styles.mono}>bank: {m.externalItemId}</Text>
                <Text style={styles.mono}>book: {m.internalItemId}</Text>
                <Text style={styles.sub}>Status: {m.status}</Text>
                <View style={styles.actions}>
                  <Button title="Confirm" onPress={() => confirm.mutate(m.id)} loading={confirm.isPending} />
                  <Button title="Reject" variant="secondary" onPress={() => reject.mutate(m.id)} loading={reject.isPending} />
                </View>
              </Card>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  list: { gap: theme.space(3) },
  matchTitle: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  mono: { color: theme.color.textMuted, fontSize: theme.font.caption, fontFamily: "monospace" },
  actions: { flexDirection: "row", gap: theme.space(3) },
});

import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import type { PayoutResponse } from "../../shared/types";
import { theme } from "../../theme/theme";
import { usePayouts } from "./hooks";

/** List payouts for the tenant with their status and risk level. */
export function PayoutListScreen(): React.ReactElement {
  const payouts = usePayouts();
  return (
    <Screen>
      <QueryState query={payouts} emptyWhen={(l) => l.length === 0} emptyText="No payouts yet.">
        {(list: PayoutResponse[]) => (
          <View style={styles.list}>
            {list.map((p) => (
              <Card key={p.id}>
                <View style={styles.row}>
                  <Text style={styles.party}>{p.partyType} · {p.partyId}</Text>
                  <Money minor={p.amountMinor} size="body" />
                </View>
                <Text style={styles.sub}>
                  {p.status} · {p.riskLevel} risk{p.provider ? ` · ${p.provider}` : ""}
                </Text>
              </Card>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  party: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "700", flexShrink: 1, paddingRight: theme.space(2) },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
});

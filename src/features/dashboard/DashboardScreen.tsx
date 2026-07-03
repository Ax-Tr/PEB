import React, { useEffect } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { syncPushRegistration } from "../../shared/push";
import type { StreamFreshness } from "../../shared/types";
import { theme } from "../../theme/theme";
import {
  useCashflow,
  useCommitmentSummary,
  useFreshness,
  usePnl,
  useReceivablesAging,
  useUpcomingObligations,
} from "./hooks";

/** Owner home: this month's P&L, cashflow, receivables — served from analytics read-models. */
export function DashboardScreen(): React.ReactElement {
  const pnl = usePnl();
  const cashflow = useCashflow();
  const receivables = useReceivablesAging();
  const freshness = useFreshness();
  const commitments = useCommitmentSummary();
  const upcoming = useUpcomingObligations(7);

  // Register for push + persist the token to notification-service once the user is in the app
  // (fail-soft; no-op on web / without native config).
  useEffect(() => {
    void syncPushRegistration();
  }, []);

  return (
    <Screen>
      <View style={styles.header}>
        <Text testID="dashboard-title" style={styles.hello}>This month</Text>
        <FreshnessBadge query={freshness} />
      </View>

      <QueryState query={pnl}>
        {(p) => (
          <Card title="Profit & loss (indicative)">
            <View style={styles.row}>
              <Metric label="Revenue" value={<Money minor={p.revenueMinor} size="h2" />} />
              <Metric label="Net profit" value={<Money minor={p.netProfitMinor} size="h2" signed />} />
            </View>
            <Text style={styles.sub}>
              Gross margin {p.grossMarginPct}% · Net margin {p.netMarginPct}%
            </Text>
          </Card>
        )}
      </QueryState>

      <QueryState query={cashflow}>
        {(c) => (
          <Card title="Cashflow">
            <View style={styles.row}>
              <Metric label="Inflow" value={<Money minor={c.totalInflowMinor} />} />
              <Metric label="Outflow" value={<Money minor={c.totalOutflowMinor} />} />
              <Metric label="Net" value={<Money minor={c.netMinor} signed />} />
            </View>
          </Card>
        )}
      </QueryState>

      <QueryState query={receivables}>
        {(a) => (
          <Card title="Receivables outstanding">
            <Money minor={a.totalOutstandingMinor} size="h2" />
            <Text style={styles.sub}>{a.totalCount} open item(s) · aged by invoice date</Text>
          </Card>
        )}
      </QueryState>
      <QueryState query={commitments}>
        {(s) => (
          <Card title="Commitments">
            <View style={styles.row}>
              <Metric label="Due today" value={<Money minor={s.dueTodayMinor} />} />
              <Metric label="Overdue" value={<Money minor={s.overdueMinor} />} />
              <Metric label="Broken" value={<Text style={styles.metricText}>{s.brokenCount}</Text>} />
            </View>
            <Text style={styles.sub}>
              {s.openCount} open · <MoneyText minor={s.openOutstandingMinor} /> outstanding
            </Text>
          </Card>
        )}
      </QueryState>

      <QueryState query={upcoming}>
        {(items) => (
          <Card title="Upcoming obligations">
            {items.length === 0 ? (
              <Text style={styles.sub}>No commitments due in the next 7 days.</Text>
            ) : (
              items.slice(0, 3).map((item) => (
                <View key={item.commitmentId} style={styles.itemRow}>
                  <Text style={styles.itemText}>{item.counterpartyName ?? item.counterpartyType}</Text>
                  <Money minor={item.outstandingMinor} size="body" />
                </View>
              ))
            )}
          </Card>
        )}
      </QueryState>
    </Screen>
  );
}

function MoneyText({ minor }: { minor: number }): React.ReactElement {
  return <Money minor={minor} size="body" />;
}

function Metric({ label, value }: { label: string; value: React.ReactNode }): React.ReactElement {
  return (
    <View style={styles.metric}>
      <Text style={styles.metricLabel}>{label}</Text>
      {value}
    </View>
  );
}

function FreshnessBadge({
  query,
}: {
  query: ReturnType<typeof useFreshness>;
}): React.ReactElement {
  const data = query.data as StreamFreshness[] | undefined;
  const stale = data?.some((s) => s.state === "STALE") ?? false;
  const color = stale ? theme.color.warning : theme.color.success;
  const label = query.isLoading ? "…" : stale ? "Updating" : "Live";
  return (
    <View style={[styles.badge, { borderColor: color }]}>
      <View style={[styles.dot, { backgroundColor: color }]} />
      <Text style={[styles.badgeText, { color }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  hello: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  row: { flexDirection: "row", gap: theme.space(4), flexWrap: "wrap" },
  metric: { gap: theme.space(1), minWidth: 90 },
  metricLabel: { color: theme.color.textMuted, fontSize: theme.font.caption },
  metricText: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  itemRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  itemText: { color: theme.color.text, fontSize: theme.font.body, flexShrink: 1, paddingRight: theme.space(2) },
  badge: {
    flexDirection: "row",
    alignItems: "center",
    gap: theme.space(1),
    borderWidth: 1,
    borderRadius: theme.radius.pill,
    paddingHorizontal: theme.space(2),
    paddingVertical: theme.space(1),
  },
  dot: { width: 8, height: 8, borderRadius: 4 },
  badgeText: { fontSize: theme.font.caption, fontWeight: "700" },
});

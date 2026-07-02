import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { BarChart } from "../../components/BarChart";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { formatINR, toRupees } from "../../shared/money";
import type { Aging, Cashflow, ProductProfitability } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useCashflow, useReceivablesAging } from "./hooks";
import { usePayablesAging, useProductProfitability } from "./detailHooks";

const BUCKET_LABEL: Record<string, string> = {
  DAYS_0_30: "0–30d",
  DAYS_31_60: "31–60d",
  DAYS_61_90: "61–90d",
  DAYS_90_PLUS: "90d+",
};

/** Deeper analytics: receivables & payables aging, cashflow by period, product profitability. */
export function InsightsScreen(): React.ReactElement {
  const receivables = useReceivablesAging();
  const payables = usePayablesAging();
  const cashflow = useCashflow();
  const products = useProductProfitability();

  return (
    <Screen>
      <Text style={styles.title}>Insights</Text>

      <QueryState query={receivables}>
        {(a: Aging) => <AgingCard title="Receivables aging" aging={a} />}
      </QueryState>
      <QueryState query={payables}>
        {(a: Aging) => <AgingCard title="Payables aging" aging={a} />}
      </QueryState>

      <QueryState query={cashflow}>
        {(c: Cashflow) => (
          <Card title="Cashflow by month">
            {c.periods.length === 0 ? (
              <Text style={styles.muted}>No cash movements yet.</Text>
            ) : (
              <>
                <BarChart
                  data={c.periods.map((p) => ({ label: `${p.month}/${String(p.year).slice(2)}`, value: toRupees(p.netMinor) }))}
                  format={(v) => formatINR(Math.round(v * 100))}
                />
                {c.periods.map((p) => (
                  <View key={`${p.year}-${p.month}`} style={styles.line}>
                    <Text style={styles.label}>
                      {p.month}/{p.year}
                    </Text>
                    <Money minor={p.netMinor} size="body" signed />
                  </View>
                ))}
              </>
            )}
          </Card>
        )}
      </QueryState>

      <QueryState
        query={products}
        emptyWhen={(l) => l.length === 0}
        emptyText="Product profitability appears once invoices carry line-level detail."
      >
        {(list: ProductProfitability[]) => (
          <Card title="Most profitable">
            {list.slice(0, 8).map((p) => (
              <View key={p.productId} style={styles.line}>
                <Text style={styles.label} numberOfLines={1}>
                  {p.productName} · {p.marginPct}%
                </Text>
                <Money minor={p.profitMinor} size="body" signed />
              </View>
            ))}
          </Card>
        )}
      </QueryState>
    </Screen>
  );
}

function AgingCard({ title, aging }: { title: string; aging: Aging }): React.ReactElement {
  return (
    <Card title={title}>
      {aging.buckets.map((b) => (
        <View key={b.bucket} style={styles.line}>
          <Text style={styles.label}>
            {BUCKET_LABEL[b.bucket] ?? b.bucket} ({b.count})
          </Text>
          <Money minor={b.totalMinor} size="body" />
        </View>
      ))}
      <View style={[styles.line, styles.totalRow]}>
        <Text style={styles.totalLabel}>Total outstanding</Text>
        <Money minor={aging.totalOutstandingMinor} size="title" />
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  line: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: theme.space(1) },
  totalRow: { borderTopWidth: 1, borderTopColor: theme.color.border, marginTop: theme.space(1), paddingTop: theme.space(2) },
  label: { color: theme.color.textMuted, fontSize: theme.font.body, flexShrink: 1, paddingRight: theme.space(2) },
  totalLabel: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "700" },
  muted: { color: theme.color.textMuted, fontSize: theme.font.caption },
});

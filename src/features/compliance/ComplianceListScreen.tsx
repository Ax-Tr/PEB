import React, { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { ApiError } from "../../shared/http";
import type { ComplianceReport } from "../../shared/types";
import { theme } from "../../theme/theme";
import type { BooksStackParamList } from "../../navigation/types";
import { REPORT_TYPES, useGenerateReport, useReports } from "./hooks";
import { statusColor } from "./statusColor";

type Props = StackScreenProps<BooksStackParamList, "Compliance">;

/** List compliance reports and generate a new one for the current period. */
export function ComplianceListScreen({ navigation }: Props): React.ReactElement {
  const reports = useReports();
  const generate = useGenerateReport();
  const [type, setType] = useState<string>(REPORT_TYPES[0]);
  const now = new Date();

  const onGenerate = () => generate.mutate({ type, year: now.getFullYear(), month: now.getMonth() + 1 });

  return (
    <Screen>
      <Card title="Generate report">
        <View style={styles.chips}>
          {REPORT_TYPES.map((t) => (
            <Pressable
              key={t}
              accessibilityRole="button"
              onPress={() => setType(t)}
              style={[styles.chip, t === type && styles.chipOn]}
            >
              <Text style={[styles.chipText, t === type && styles.chipTextOn]}>{label(t)}</Text>
            </Pressable>
          ))}
        </View>
        {generate.isError ? (
          <Text style={styles.error}>{generate.error instanceof ApiError ? generate.error.message : "Could not generate."}</Text>
        ) : null}
        <Button title={`Generate for ${now.getMonth() + 1}/${now.getFullYear()}`} onPress={onGenerate} loading={generate.isPending} />
      </Card>

      <QueryState query={reports} emptyWhen={(l) => l.length === 0} emptyText="No reports yet.">
        {(list: ComplianceReport[]) => (
          <View style={styles.list}>
            {list.map((r) => (
              <Pressable
                key={r.id}
                accessibilityRole="button"
                onPress={() => navigation.navigate("ComplianceDetail", { reportId: r.id, title: label(r.type) })}
              >
                <Card>
                  <View style={styles.row}>
                    <Text style={styles.name}>{label(r.type)}</Text>
                    <StatusPill state={r.displayState} />
                  </View>
                  <Text style={styles.sub}>
                    {r.month}/{r.year}
                    {r.missingFields.length > 0 ? ` · ${r.missingFields.length} flag(s)` : ""}
                  </Text>
                </Card>
              </Pressable>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

export function StatusPill({ state }: { state: string }): React.ReactElement {
  const color = statusColor(state);
  return (
    <View style={[styles.pill, { borderColor: color }]}>
      <Text style={[styles.pillText, { color }]}>{state}</Text>
    </View>
  );
}

function label(type: string): string {
  return type.replace(/_/g, " ").replace("SUMMARY", "").trim();
}

const styles = StyleSheet.create({
  chips: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  chip: { borderWidth: 1, borderColor: theme.color.border, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(3), paddingVertical: theme.space(2) },
  chipOn: { backgroundColor: theme.color.primary, borderColor: theme.color.primary },
  chipText: { color: theme.color.textMuted, fontSize: theme.font.caption },
  chipTextOn: { color: theme.color.primaryText, fontWeight: "700" },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  name: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
  pill: { borderWidth: 1, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(2), paddingVertical: theme.space(1) },
  pillText: { fontSize: theme.font.caption, fontWeight: "700" },
});

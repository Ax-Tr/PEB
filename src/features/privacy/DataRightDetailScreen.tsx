import React, { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import type { DsrRequest, ErasurePlanResult } from "../../shared/types";
import { theme } from "../../theme/theme";
import type { MoreStackParamList } from "../../navigation/types";
import { DATA_CATEGORIES, useDsrLifecycle, useDsrRequest, useErasurePlan } from "./hooks";

type Props = StackScreenProps<MoreStackParamList, "DataRightDetail">;

const ACTION_COLOR: Record<string, string> = {
  DELETE: theme.color.success,
  ANONYMIZE: theme.color.warning,
  RETAIN_LEGAL_HOLD: theme.color.danger,
};

/** Work a DSR through its lifecycle: verify identity → plan erasure → complete / reject. */
export function DataRightDetailScreen({ route }: Props): React.ReactElement {
  const { requestId } = route.params;
  const request = useDsrRequest(requestId);
  const lifecycle = useDsrLifecycle();
  const plan = useErasurePlan();
  const [ack, setAck] = useState("");
  const [categories, setCategories] = useState<string[]>(["MARKETING", "FINANCIAL_TXN", "KYC_PII"]);

  const toggle = (c: string) =>
    setCategories((prev) => (prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c]));

  const planResult: ErasurePlanResult | undefined = plan.data;

  return (
    <Screen>
      <QueryState query={request}>
        {(r: DsrRequest) => (
          <>
            <Card>
              <View style={styles.row}>
                <Text style={styles.title}>{r.type}</Text>
                <Text style={styles.status}>{r.status}</Text>
              </View>
              <Text style={styles.sub}>Requester: {r.subjectEmail}</Text>
              <Text style={styles.sub}>Due {new Date(r.dueAt).toLocaleString()}</Text>
              {r.resolutionNote ? <Text style={styles.sub}>Note: {r.resolutionNote}</Text> : null}
            </Card>

            <Card title="Workflow">
              {lifecycle.isError ? (
                <Text style={styles.error}>{lifecycle.error instanceof ApiError ? lifecycle.error.message : "Action failed."}</Text>
              ) : null}
              {r.status === "RECEIVED" ? (
                <Button title="Start verification" onPress={() => lifecycle.mutate({ id: r.id, action: "start-verification" })} loading={lifecycle.isPending} />
              ) : null}
              {r.status === "VERIFYING" ? (
                <Button title="Confirm requester identity" onPress={() => lifecycle.mutate({ id: r.id, action: "verify" })} loading={lifecycle.isPending} />
              ) : null}
              {r.status === "IN_PROGRESS" ? (
                <>
                  <TextField label="Evidence reference" value={ack} onChangeText={setAck} placeholder="s3://fulfilment-evidence" />
                  <Button
                    title="Complete request"
                    onPress={() => lifecycle.mutate({ id: r.id, action: "complete", body: { evidenceRef: ack.trim() } })}
                    loading={lifecycle.isPending}
                    disabled={ack.trim().length === 0}
                  />
                </>
              ) : null}
              {r.status !== "COMPLETED" && r.status !== "REJECTED" ? (
                <Button title="Reject" variant="secondary" onPress={() => lifecycle.mutate({ id: r.id, action: "reject", body: { reason: "Could not verify" } })} loading={lifecycle.isPending} />
              ) : null}
            </Card>

            {r.type === "ERASURE" ? (
              <Card title="Erasure plan">
                <Text style={styles.sub}>Choose categories; the plan is honest about what must be retained.</Text>
                <View style={styles.chips}>
                  {DATA_CATEGORIES.map((c) => (
                    <Pressable key={c} onPress={() => toggle(c)} accessibilityRole="button" style={[styles.chip, categories.includes(c) && styles.chipOn]}>
                      <Text style={[styles.chipText, categories.includes(c) && styles.chipTextOn]}>{c}</Text>
                    </Pressable>
                  ))}
                </View>
                <Button title="Compute plan" onPress={() => plan.mutate({ id: r.id, categories })} loading={plan.isPending} disabled={categories.length === 0} />
                {planResult ? (
                  <View style={styles.planBox}>
                    <Text style={[styles.summary, { color: planResult.fullErasurePossible ? theme.color.success : theme.color.warning }]}>
                      {planResult.summary}
                    </Text>
                    {planResult.lines.map((l) => (
                      <View key={l.category} style={styles.row}>
                        <Text style={styles.label}>{l.category}</Text>
                        <Text style={[styles.action, { color: ACTION_COLOR[l.action] ?? theme.color.textMuted }]}>{l.action}</Text>
                      </View>
                    ))}
                  </View>
                ) : null}
              </Card>
            ) : null}
          </>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: theme.space(1) },
  title: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "800" },
  status: { color: theme.color.textMuted, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  chip: { borderWidth: 1, borderColor: theme.color.border, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(2), paddingVertical: theme.space(1) },
  chipOn: { backgroundColor: theme.color.surfaceAlt, borderColor: theme.color.primary },
  chipText: { color: theme.color.textMuted, fontSize: theme.font.caption },
  chipTextOn: { color: theme.color.text, fontWeight: "700" },
  planBox: { gap: theme.space(1), marginTop: theme.space(2) },
  summary: { fontSize: theme.font.body, fontWeight: "700" },
  label: { color: theme.color.textMuted, fontSize: theme.font.caption },
  action: { fontSize: theme.font.caption, fontWeight: "700" },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

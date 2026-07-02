import React, { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import type { DsrRequest } from "../../shared/types";
import { theme } from "../../theme/theme";
import type { MoreStackParamList } from "../../navigation/types";
import { DSR_TYPES, useDsrRequests, useSubmitDsr } from "./hooks";

type Props = StackScreenProps<MoreStackParamList, "DataRights">;

/** DPDP data-principal requests: raise one and track existing requests to their SLA. */
export function DataRightsScreen({ navigation }: Props): React.ReactElement {
  const list = useDsrRequests();
  const submit = useSubmitDsr();
  const [type, setType] = useState<string>("ERASURE");
  const [email, setEmail] = useState("");
  const [details, setDetails] = useState("");

  const onSubmit = () =>
    submit.mutate(
      { type, subjectEmail: email.trim(), details: details.trim() || undefined },
      { onSuccess: () => setEmail("") },
    );

  return (
    <Screen>
      <Card title="Raise a data request">
        <View style={styles.chips}>
          {DSR_TYPES.map((t) => (
            <Pressable key={t} onPress={() => setType(t)} accessibilityRole="button" style={[styles.chip, t === type && styles.chipOn]}>
              <Text style={[styles.chipText, t === type && styles.chipTextOn]}>{t}</Text>
            </Pressable>
          ))}
        </View>
        <TextField label="Data principal email" value={email} onChangeText={setEmail} placeholder="person@example.com" />
        <TextField label="Details (optional)" value={details} onChangeText={setDetails} placeholder="What is being requested?" />
        {submit.isError ? (
          <Text style={styles.error}>{submit.error instanceof ApiError ? submit.error.message : "Could not submit."}</Text>
        ) : null}
        <Button title="Submit request" onPress={onSubmit} loading={submit.isPending} disabled={email.length < 3} />
      </Card>

      <QueryState query={list} emptyWhen={(l) => l.length === 0} emptyText="No data requests yet.">
        {(rows: DsrRequest[]) => (
          <View style={styles.list}>
            {rows.map((r) => (
              <Pressable key={r.id} accessibilityRole="button" onPress={() => navigation.navigate("DataRightDetail", { requestId: r.id })}>
                <Card>
                  <View style={styles.row}>
                    <Text style={styles.name}>{r.type}</Text>
                    <Text style={styles.status}>{r.status}</Text>
                  </View>
                  <Text style={styles.sub}>Due {new Date(r.dueAt).toLocaleDateString()}</Text>
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
  chips: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  chip: { borderWidth: 1, borderColor: theme.color.border, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(3), paddingVertical: theme.space(1) },
  chipOn: { backgroundColor: theme.color.primary, borderColor: theme.color.primary },
  chipText: { color: theme.color.textMuted, fontSize: theme.font.caption },
  chipTextOn: { color: theme.color.primaryText, fontWeight: "700" },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  name: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  status: { color: theme.color.textMuted, fontSize: theme.font.caption, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

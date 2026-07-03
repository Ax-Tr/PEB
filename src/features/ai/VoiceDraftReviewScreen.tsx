import React, { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import type { MoreStackParamList } from "../../navigation/types";
import { ApiError } from "../../shared/http";
import type { VoiceDraft } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useApproveVoiceDraft, useRejectVoiceDraft, useVoiceDrafts } from "./hooks";

type Props = StackScreenProps<MoreStackParamList, "VoiceDraftReview">;

export function VoiceDraftReviewScreen({ route }: Props): React.ReactElement {
  const drafts = useVoiceDrafts("NEEDS_REVIEW");
  const approve = useApproveVoiceDraft();
  const reject = useRejectVoiceDraft();
  const [selectedId, setSelectedId] = useState(route.params?.draftId ?? null);
  const selected = useMemo(
    () => drafts.data?.find((d) => d.id === selectedId) ?? drafts.data?.[0],
    [drafts.data, selectedId],
  );
  const [fields, setFields] = useState<Record<string, string>>({});

  useEffect(() => {
    if (selected) {
      setFields(flattenFields(selected.fields));
    }
  }, [selected]);

  const update = (key: string, value: string) => setFields((prev) => ({ ...prev, [key]: value }));
  const canApprove =
    selected &&
    !selected.suspicious &&
    selected.status === "NEEDS_REVIEW" &&
    requiredFor(selected).every((key) => fields[key]?.trim().length > 0);

  return (
    <Screen>
      <Text style={styles.title}>Voice drafts</Text>
      <QueryState query={drafts} emptyWhen={(l) => l.length === 0} emptyText="No voice drafts awaiting review.">
        {(list: VoiceDraft[]) => (
          <View style={styles.content}>
            <View style={styles.tabs}>
              {list.map((draft) => (
                <Pressable key={draft.id} onPress={() => setSelectedId(draft.id)} style={[styles.tab, draft.id === selected?.id && styles.tabOn]}>
                  <Text style={styles.tabText}>{draft.intent.replace(/_/g, " ")}</Text>
                </Pressable>
              ))}
            </View>
            {selected ? (
              <Card title={selected.intent.replace(/_/g, " ")}>
                <Text style={styles.transcript}>{selected.transcript}</Text>
                <Text style={[styles.meta, selected.suspicious && styles.danger]}>
                  Confidence {(Number(selected.confidence) * 100).toFixed(0)}%
                  {selected.suspicious ? " · suspicious transcript blocked" : ""}
                </Text>
                {selected.missingFields.length > 0 ? (
                  <Text style={styles.warn}>Missing: {selected.missingFields.join(", ")}</Text>
                ) : null}
                {Object.entries(fields).map(([key, value]) => (
                  <TextField key={key} label={key} value={value} onChangeText={(v) => update(key, v)} />
                ))}
                {requiredFor(selected)
                  .filter((key) => !(key in fields))
                  .map((key) => (
                    <TextField key={key} label={key} value="" onChangeText={(v) => update(key, v)} />
                  ))}
                <View style={styles.actions}>
                  <Button
                    title="Approve and create"
                    onPress={() => approve.mutate({ id: selected.id, fields })}
                    loading={approve.isPending}
                    disabled={!canApprove}
                  />
                  <Button
                    title="Reject"
                    variant="secondary"
                    onPress={() => reject.mutate({ id: selected.id, reason: "Rejected in review" })}
                    loading={reject.isPending}
                  />
                </View>
                {approve.data ? <Text style={styles.success}>Created record {approve.data.materializedRef}</Text> : null}
                {approve.error ? <Text style={styles.danger}>{approve.error instanceof ApiError ? approve.error.message : "Approve failed."}</Text> : null}
              </Card>
            ) : null}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

function flattenFields(fields: Record<string, unknown>): Record<string, string> {
  return Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, String(value ?? "")]));
}

function requiredFor(draft: VoiceDraft): string[] {
  return draft.intent === "CREATE_COMMITMENT" ? ["counterpartyName", "amountMinor", "dueDate"] : draft.missingFields;
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  content: { gap: theme.space(3) },
  tabs: { gap: theme.space(2) },
  tab: { padding: theme.space(3), borderRadius: theme.radius.md, borderWidth: 1, borderColor: theme.color.border },
  tabOn: { backgroundColor: theme.color.surfaceAlt },
  tabText: { color: theme.color.text, fontSize: theme.font.caption, fontWeight: "700" },
  transcript: { color: theme.color.text, fontSize: theme.font.body },
  meta: { color: theme.color.textMuted, fontSize: theme.font.caption },
  warn: { color: theme.color.warning, fontSize: theme.font.caption },
  danger: { color: theme.color.danger, fontSize: theme.font.caption },
  success: { color: theme.color.success, fontSize: theme.font.caption },
  actions: { gap: theme.space(2) },
});

import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import type { AiSuggestion, AnomalyAlert } from "../../shared/types";
import { theme } from "../../theme/theme";
import { API_PREFIX } from "../../shared/constants";
import { useOfflineQueue } from "../offline/OfflineQueueContext";
import {
  useAiSuggestions,
  useAnomalies,
  useAskAssistant,
  useDecideAnomaly,
  useDecideSuggestion,
} from "./hooks";

/** AI: ask the (governed) assistant, review suggestions with their confidence, and triage anomalies. */
export function AssistantScreen(): React.ReactElement {
  const [question, setQuestion] = useState("");
  const ask = useAskAssistant();
  const suggestions = useAiSuggestions("PROPOSED");
  const accept = useDecideSuggestion("accept");
  const reject = useDecideSuggestion("reject");
  const anomalies = useAnomalies("OPEN");
  const ackAnomaly = useDecideAnomaly("acknowledge");
  const dismissAnomaly = useDecideAnomaly("dismiss");
  const { postOrQueue } = useOfflineQueue();
  const [feedbackNote, setFeedbackNote] = useState<string | null>(null);

  const sendFeedback = async (id: string, helpful: boolean) => {
    const res = await postOrQueue(`AI feedback ${id}`, `${API_PREFIX}/ai/suggestions/${id}/feedback`, {
      helpful,
      note: null,
    });
    setFeedbackNote(res.queued ? "Feedback saved — will sync when online." : "Thanks for the feedback.");
  };

  return (
    <Screen>
      <Card title="Ask the finance assistant">
        <TextField label="Question" value={question} onChangeText={setQuestion} placeholder="e.g. How is my cash position?" />
        <Button title="Ask" onPress={() => ask.mutate(question.trim())} loading={ask.isPending} disabled={question.trim().length === 0} />
        {ask.data ? (
          <View style={styles.answer}>
            <Text style={styles.answerText}>{ask.data.answer}</Text>
            <Text style={styles.meta}>
              {ask.data.modelAvailable ? `Confidence ${(ask.data.confidence * 100).toFixed(0)}%` : "Manual review (assistant offline)"}
              {ask.data.injectionDetected ? " · ⚠ suspicious input neutralised" : ""}
            </Text>
          </View>
        ) : null}
        {ask.isError ? <Text style={styles.error}>{ask.error instanceof ApiError ? ask.error.message : "Ask failed."}</Text> : null}
      </Card>

      <Text style={styles.section}>Suggestions to review</Text>
      {feedbackNote ? <Text style={styles.note}>{feedbackNote}</Text> : null}
      <QueryState query={suggestions} emptyWhen={(l) => l.length === 0} emptyText="No suggestions awaiting review.">
        {(list: AiSuggestion[]) => (
          <View style={styles.list}>
            {list.map((s) => (
              <Card key={s.id}>
                <View style={styles.row}>
                  <Text style={styles.kind}>{s.kind.replace(/_/g, " ")}</Text>
                  <Text style={styles.conf}>conf {(Number(s.confidence) * 100).toFixed(0)}%</Text>
                </View>
                <Text style={styles.sub}>Decision: {s.decision}</Text>
                <View style={styles.actions}>
                  <Button title="Accept" onPress={() => accept.mutate(s.id)} loading={accept.isPending} />
                  <Button title="Reject" variant="secondary" onPress={() => reject.mutate(s.id)} loading={reject.isPending} />
                </View>
                <View style={styles.actions}>
                  <Button title="👍 Helpful" variant="secondary" onPress={() => void sendFeedback(s.id, true)} />
                  <Button title="👎 Not helpful" variant="secondary" onPress={() => void sendFeedback(s.id, false)} />
                </View>
              </Card>
            ))}
          </View>
        )}
      </QueryState>

      <Text style={styles.section}>Anomaly alerts</Text>
      <QueryState query={anomalies} emptyWhen={(l) => l.length === 0} emptyText="No open anomaly alerts.">
        {(list: AnomalyAlert[]) => (
          <View style={styles.list}>
            {list.map((a) => (
              <Card key={a.id}>
                <View style={styles.row}>
                  <Text style={styles.kind}>{a.subjectType}</Text>
                  <Text style={[styles.severity, { color: sevColor(a.severity) }]}>{a.severity}</Text>
                </View>
                <View style={styles.row}>
                  <Text style={styles.sub}>{a.detail}</Text>
                  <Money minor={a.observedMinor} size="body" />
                </View>
                <View style={styles.actions}>
                  <Button title="Acknowledge" onPress={() => ackAnomaly.mutate(a.id)} loading={ackAnomaly.isPending} />
                  <Button title="Dismiss" variant="secondary" onPress={() => dismissAnomaly.mutate(a.id)} loading={dismissAnomaly.isPending} />
                </View>
              </Card>
            ))}
          </View>
        )}
      </QueryState>
    </Screen>
  );
}

function sevColor(sev: string): string {
  return sev === "HIGH" ? theme.color.danger : sev === "MEDIUM" ? theme.color.warning : theme.color.textMuted;
}

const styles = StyleSheet.create({
  section: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "800", marginTop: theme.space(2) },
  list: { gap: theme.space(3) },
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  kind: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "700" },
  conf: { color: theme.color.primary, fontSize: theme.font.caption, fontWeight: "700" },
  severity: { fontSize: theme.font.caption, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption, flexShrink: 1, paddingRight: theme.space(2) },
  actions: { flexDirection: "row", gap: theme.space(3) },
  answer: { backgroundColor: theme.color.surfaceAlt, borderRadius: theme.radius.md, padding: theme.space(3), gap: theme.space(1) },
  answerText: { color: theme.color.text, fontSize: theme.font.body },
  meta: { color: theme.color.textMuted, fontSize: theme.font.caption },
  note: { color: theme.color.success, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import type { BooksStackParamList } from "../../navigation/types";
import { ApiError } from "../../shared/http";
import { parseRupeesToMinor } from "../../shared/money";
import { theme } from "../../theme/theme";
import { useCancelCommitment, useCommitment, useRecordCommitmentPayment, useRescheduleCommitment } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "CommitmentDetail">;

/** Commitment detail with timeline, payment recording, reschedule, and cancellation actions. */
export function CommitmentDetailScreen({ route }: Props): React.ReactElement {
  const { commitmentId } = route.params;
  const detail = useCommitment(commitmentId);
  const recordPayment = useRecordCommitmentPayment(commitmentId);
  const reschedule = useRescheduleCommitment(commitmentId);
  const cancel = useCancelCommitment(commitmentId);
  const [paymentAmount, setPaymentAmount] = useState("");
  const [newDueDate, setNewDueDate] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | undefined>();

  const onPay = () => {
    setError(undefined);
    try {
      recordPayment.mutate({ amountMinor: parseRupeesToMinor(paymentAmount), note: note.trim() || undefined });
    } catch {
      setError("Enter a valid payment amount.");
    }
  };

  return (
    <Screen>
      <QueryState query={detail}>
        {(data) => {
          const c = data.commitment;
          return (
            <>
              <Card title="Promise">
                <View style={styles.row}>
                  <Text style={styles.name}>{c.counterpartyName ?? c.counterpartyType}</Text>
                  <Text style={styles.status}>{c.status.replace("_", " ")}</Text>
                </View>
                <View style={styles.row}>
                  <Metric label="Promised" value={<Money minor={c.amountMinor} size="title" />} />
                  <Metric label="Outstanding" value={<Money minor={c.outstandingMinor} size="title" />} />
                </View>
                <Text style={styles.sub}>Due {formatDate(c.dueDate)}</Text>
                {c.description ? <Text style={styles.sub}>{c.description}</Text> : null}
              </Card>

              {c.status !== "PAID" && c.status !== "CANCELLED" ? (
                <Card title="Update">
                  <TextField label="Payment amount (₹)" value={paymentAmount} onChangeText={setPaymentAmount} placeholder="0.00" keyboardType="decimal-pad" error={error} />
                  <TextField label="Note" value={note} onChangeText={setNote} placeholder="e.g. UPI received" />
                  {recordPayment.isError ? <Text style={styles.error}>{message(recordPayment.error)}</Text> : null}
                  <Button title="Record payment" onPress={onPay} loading={recordPayment.isPending} disabled={!paymentAmount} />

                  <TextField label="New due date" value={newDueDate} onChangeText={setNewDueDate} placeholder="YYYY-MM-DD" />
                  {reschedule.isError ? <Text style={styles.error}>{message(reschedule.error)}</Text> : null}
                  <Button
                    title="Reschedule"
                    variant="secondary"
                    onPress={() => reschedule.mutate({ newDueDate, note: note.trim() || undefined })}
                    loading={reschedule.isPending}
                    disabled={!/^\d{4}-\d{2}-\d{2}$/.test(newDueDate)}
                  />
                  {cancel.isError ? <Text style={styles.error}>{message(cancel.error)}</Text> : null}
                  <Button title="Cancel commitment" variant="secondary" onPress={() => cancel.mutate({ note: note.trim() || undefined })} loading={cancel.isPending} />
                </Card>
              ) : null}

              <Card title="Timeline">
                {data.events.length === 0 ? (
                  <Text style={styles.sub}>No events yet.</Text>
                ) : (
                  data.events.map((e) => (
                    <View key={e.id} style={styles.event}>
                      <Text style={styles.eventTitle}>{e.eventType.replace(/_/g, " ")}</Text>
                      <Text style={styles.sub}>
                        {new Date(e.occurredAt).toLocaleString()}
                        {e.amountMinor ? ` · ₹${(e.amountMinor / 100).toFixed(2)}` : ""}
                      </Text>
                      {e.note ? <Text style={styles.sub}>{e.note}</Text> : null}
                    </View>
                  ))
                )}
              </Card>
            </>
          );
        }}
      </QueryState>
    </Screen>
  );
}

function Metric({ label, value }: { label: string; value: React.ReactNode }): React.ReactElement {
  return (
    <View style={styles.metric}>
      <Text style={styles.sub}>{label}</Text>
      {value}
    </View>
  );
}

function message(error: unknown): string {
  return error instanceof ApiError ? error.message : "Action failed.";
}

function formatDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString();
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: theme.space(3), flexWrap: "wrap" },
  name: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800", flexShrink: 1 },
  status: { color: theme.color.primary, fontSize: theme.font.caption, fontWeight: "800" },
  metric: { minWidth: 120, gap: theme.space(1) },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
  event: { borderTopWidth: 1, borderTopColor: theme.color.border, paddingTop: theme.space(2), gap: theme.space(1) },
  eventTitle: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "700" },
});

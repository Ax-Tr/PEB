import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import type { BooksStackParamList } from "../../navigation/types";
import { ApiError } from "../../shared/http";
import { parseRupeesToMinor } from "../../shared/money";
import { theme } from "../../theme/theme";
import { useCreateCommitment } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "CommitmentCreate">;

const TYPES = ["CUSTOMER", "VENDOR", "EMPLOYEE", "OTHER"] as const;

/** Create a payment promise with amount and due date. */
export function CommitmentCreateScreen({ navigation }: Props): React.ReactElement {
  const [counterpartyType, setCounterpartyType] = useState<(typeof TYPES)[number]>("CUSTOMER");
  const [counterpartyName, setCounterpartyName] = useState("");
  const [amount, setAmount] = useState("");
  const [dueDate, setDueDate] = useState(todayIso());
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | undefined>();
  const create = useCreateCommitment();

  const onCreate = () => {
    setError(undefined);
    let amountMinor: number;
    try {
      amountMinor = parseRupeesToMinor(amount);
    } catch {
      setError("Enter a valid amount, e.g. 5000.00");
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dueDate)) {
      setError("Enter due date as YYYY-MM-DD");
      return;
    }
    create.mutate(
      {
        counterpartyType,
        counterpartyName: counterpartyName.trim(),
        sourceType: "MANUAL",
        description: description.trim() || undefined,
        amountMinor,
        dueDate,
      },
      {
        onSuccess: (c) =>
          navigation.replace("CommitmentDetail", {
            commitmentId: c.id,
            title: c.counterpartyName ?? "Commitment",
          }),
      },
    );
  };

  return (
    <Screen>
      <Text style={styles.title}>New commitment</Text>
      <Card>
        <Text style={styles.label}>Who promised?</Text>
        <View style={styles.typeRow}>
          {TYPES.map((t) => (
            <Button key={t} title={t} variant={counterpartyType === t ? "primary" : "secondary"} onPress={() => setCounterpartyType(t)} />
          ))}
        </View>
        <TextField label="Name" value={counterpartyName} onChangeText={setCounterpartyName} placeholder="e.g. Raj / Acme Traders" />
        <TextField label="Amount (₹)" value={amount} onChangeText={setAmount} placeholder="0.00" keyboardType="decimal-pad" error={error} />
        <TextField label="Due date" value={dueDate} onChangeText={setDueDate} placeholder="YYYY-MM-DD" />
        <TextField label="Note (optional)" value={description} onChangeText={setDescription} placeholder="e.g. Second course installment" />
        {create.isError ? (
          <Text style={styles.error}>{create.error instanceof ApiError ? create.error.message : "Could not create commitment."}</Text>
        ) : null}
        <Button title="Create commitment" onPress={onCreate} loading={create.isPending} disabled={!counterpartyName.trim() || !amount} />
      </Card>
    </Screen>
  );
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  label: { color: theme.color.textMuted, fontSize: theme.font.caption, fontWeight: "700" },
  typeRow: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

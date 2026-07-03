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
import { useCreateInstallment } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "InstallmentCreate">;

/** Creates a receivable/payable EMI schedule. */
export function InstallmentCreateScreen({ navigation, route }: Props): React.ReactElement {
  const [type, setType] = useState<"RECEIVABLE" | "PAYABLE">(route.params?.type ?? "RECEIVABLE");
  const [counterpartyName, setCounterpartyName] = useState("");
  const [amount, setAmount] = useState("");
  const [numberOfEmis, setNumberOfEmis] = useState("3");
  const [firstDueDate, setFirstDueDate] = useState(todayIso());
  const [frequency, setFrequency] = useState("MONTHLY");
  const [error, setError] = useState<string | undefined>();
  const create = useCreateInstallment();

  const onCreate = () => {
    setError(undefined);
    let totalAmountMinor: number;
    try {
      totalAmountMinor = parseRupeesToMinor(amount);
    } catch {
      setError("Enter a valid amount, e.g. 15000.00");
      return;
    }
    const emis = Number(numberOfEmis);
    if (!Number.isInteger(emis) || emis < 1) {
      setError("Number of EMIs must be at least 1");
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(firstDueDate)) {
      setError("Enter first due date as YYYY-MM-DD");
      return;
    }
    create.mutate(
      {
        type,
        counterpartyName: counterpartyName.trim(),
        sourceType: "MANUAL",
        totalAmountMinor,
        numberOfEmis: emis,
        firstDueDate,
        frequency,
      },
      {
        onSuccess: (item) =>
          navigation.replace("InstallmentDetail", {
            installmentId: item.id,
            title: item.counterpartyName ?? "Installment",
          }),
      },
    );
  };

  return (
    <Screen>
      <Text style={styles.title}>New installment</Text>
      <Card>
        <View style={styles.row}>
          <Button title="Receivable" variant={type === "RECEIVABLE" ? "primary" : "secondary"} onPress={() => setType("RECEIVABLE")} />
          <Button title="Payable" variant={type === "PAYABLE" ? "primary" : "secondary"} onPress={() => setType("PAYABLE")} />
        </View>
        <TextField label="Counterparty name" value={counterpartyName} onChangeText={setCounterpartyName} placeholder="e.g. Raj / Acme Traders" />
        <TextField label="Total amount (₹)" value={amount} onChangeText={setAmount} placeholder="0.00" keyboardType="decimal-pad" error={error} />
        <TextField label="Number of EMIs" value={numberOfEmis} onChangeText={setNumberOfEmis} keyboardType="number-pad" />
        <TextField label="First due date" value={firstDueDate} onChangeText={setFirstDueDate} placeholder="YYYY-MM-DD" />
        <TextField label="Frequency" value={frequency} onChangeText={setFrequency} placeholder="MONTHLY" />
        {create.isError ? <Text style={styles.error}>{create.error instanceof ApiError ? create.error.message : "Could not create schedule."}</Text> : null}
        <Button title="Create schedule" onPress={onCreate} loading={create.isPending} disabled={!counterpartyName.trim() || !amount} />
      </Card>
    </Screen>
  );
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  row: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

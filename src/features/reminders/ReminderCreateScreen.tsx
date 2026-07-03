import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import type { BooksStackParamList } from "../../navigation/types";
import { ApiError } from "../../shared/http";
import { theme } from "../../theme/theme";
import { useScheduleReminder } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "ReminderCreate">;

const CHANNELS = ["SMS", "EMAIL", "PUSH", "WHATSAPP"] as const;

/** Schedule D-3/D-1/D-day reminders for a due date. */
export function ReminderCreateScreen({ navigation, route }: Props): React.ReactElement {
  const params = route.params;
  const [channel, setChannel] = useState<(typeof CHANNELS)[number]>("SMS");
  const [templateCode, setTemplateCode] = useState("EMI_DUE");
  const [recipient, setRecipient] = useState("");
  const [dueDate, setDueDate] = useState(params?.dueDate ?? todayIso());
  const [offsets, setOffsets] = useState("3,1,0");
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | undefined>();
  const schedule = useScheduleReminder();

  const onSchedule = () => {
    setError(undefined);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dueDate)) {
      setError("Enter due date as YYYY-MM-DD");
      return;
    }
    const parsedOffsets = offsets
      .split(",")
      .map((o) => Number(o.trim()))
      .filter((o) => Number.isInteger(o) && o >= 0);
    if (parsedOffsets.length === 0) {
      setError("Enter at least one offset, e.g. 3,1,0");
      return;
    }
    schedule.mutate(
      {
        sourceType: params?.sourceType ?? "MANUAL",
        sourceRef: params?.sourceRef,
        emiNumber: params?.emiNumber,
        channel,
        templateCode,
        recipient: recipient.trim(),
        dueDate,
        offsets: parsedOffsets,
        variables: {
          name: name.trim() || "customer",
          amount: amount.trim() || "the amount",
        },
      },
      { onSuccess: () => navigation.replace("ReminderList") },
    );
  };

  return (
    <Screen>
      <Text style={styles.title}>New reminder</Text>
      <Card>
        <Text style={styles.label}>Channel</Text>
        <View style={styles.row}>
          {CHANNELS.map((c) => (
            <Button key={c} title={c} variant={channel === c ? "primary" : "secondary"} onPress={() => setChannel(c)} />
          ))}
        </View>
        <TextField label="Template code" value={templateCode} onChangeText={setTemplateCode} placeholder="EMI_DUE" />
        <TextField label="Recipient" value={recipient} onChangeText={setRecipient} placeholder="mobile, email, or push token" />
        <TextField label="Due date" value={dueDate} onChangeText={setDueDate} placeholder="YYYY-MM-DD" error={error} />
        <TextField label="Offsets" value={offsets} onChangeText={setOffsets} placeholder="3,1,0" />
        <TextField label="Name variable" value={name} onChangeText={setName} placeholder="e.g. Raj" />
        <TextField label="Amount variable" value={amount} onChangeText={setAmount} placeholder="e.g. ₹5,000" />
        {params?.sourceRef ? (
          <Text style={styles.sub}>
            Linked to {params.sourceType ?? "source"} {params.sourceRef}
            {params.emiNumber ? ` · EMI ${params.emiNumber}` : ""}
          </Text>
        ) : null}
        {schedule.isError ? <Text style={styles.error}>{schedule.error instanceof ApiError ? schedule.error.message : "Could not schedule reminder."}</Text> : null}
        <Button title="Schedule reminders" onPress={onSchedule} loading={schedule.isPending} disabled={!templateCode.trim() || !recipient.trim()} />
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
  row: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

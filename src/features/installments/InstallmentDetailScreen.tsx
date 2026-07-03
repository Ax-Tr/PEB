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
import { theme } from "../../theme/theme";
import { useCancelInstallment, useInstallment, useModifyInstallment, usePayInstallmentEmi } from "./hooks";

type Props = StackScreenProps<BooksStackParamList, "InstallmentDetail">;

/** Installment detail with EMI timeline, full-EMI payment action, reschedule, and cancellation. */
export function InstallmentDetailScreen({ navigation, route }: Props): React.ReactElement {
  const { installmentId } = route.params;
  const detail = useInstallment(installmentId);
  const pay = usePayInstallmentEmi(installmentId);
  const modify = useModifyInstallment(installmentId);
  const cancel = useCancelInstallment(installmentId);
  const [numberOfEmis, setNumberOfEmis] = useState("3");
  const [firstDueDate, setFirstDueDate] = useState("");
  const [frequency, setFrequency] = useState("MONTHLY");

  return (
    <Screen>
      <QueryState query={detail}>
        {(item) => (
          <>
            <Card title="Schedule">
              <View style={styles.row}>
                <Text style={styles.name}>{item.counterpartyName ?? item.type}</Text>
                <Text style={styles.status}>{item.status}</Text>
              </View>
              <View style={styles.row}>
                <Metric label="Total" value={<Money minor={item.totalAmountMinor} size="title" />} />
                <Metric label="Outstanding" value={<Money minor={item.outstandingMinor} size="title" />} />
              </View>
              <Text style={styles.sub}>{item.numberOfEmis} EMI(s) · {item.frequency}</Text>
            </Card>

            <Card title="EMI timeline">
              {item.emis.map((emi) => {
                const outstanding = emi.amountMinor - emi.paidMinor;
                return (
                  <View key={emi.id} style={styles.emi}>
                    <View style={styles.row}>
                      <Text style={styles.emiTitle}>EMI {emi.emiNumber}</Text>
                      <Text style={styles.status}>{emi.status}</Text>
                    </View>
                    <View style={styles.row}>
                      <Text style={styles.sub}>Due {formatDate(emi.dueDate)}</Text>
                      <Money minor={outstanding} size="body" />
                    </View>
                    {emi.status !== "PAID" ? (
                      <View style={styles.actions}>
                        <Button
                          title="Mark paid"
                          onPress={() => pay.mutate({ emiNumber: emi.emiNumber, amountMinor: outstanding })}
                          loading={pay.isPending}
                        />
                        <Button
                          title="Reminder"
                          variant="secondary"
                          onPress={() =>
                            navigation.navigate("ReminderCreate", {
                              sourceType: "INSTALLMENT_EMI",
                              sourceRef: item.id,
                              emiNumber: emi.emiNumber,
                              dueDate: emi.dueDate,
                            })
                          }
                        />
                      </View>
                    ) : null}
                  </View>
                );
              })}
              {pay.isError ? <Text style={styles.error}>{message(pay.error)}</Text> : null}
            </Card>

            {item.status === "ACTIVE" ? (
              <Card title="Reschedule balance">
                <TextField label="Number of new EMIs" value={numberOfEmis} onChangeText={setNumberOfEmis} keyboardType="number-pad" />
                <TextField label="First due date" value={firstDueDate} onChangeText={setFirstDueDate} placeholder="YYYY-MM-DD" />
                <TextField label="Frequency" value={frequency} onChangeText={setFrequency} />
                {modify.isError ? <Text style={styles.error}>{message(modify.error)}</Text> : null}
                <Button
                  title="Reschedule"
                  variant="secondary"
                  onPress={() => modify.mutate({ numberOfEmis: Number(numberOfEmis), firstDueDate, frequency })}
                  loading={modify.isPending}
                  disabled={!/^\d{4}-\d{2}-\d{2}$/.test(firstDueDate)}
                />
                {cancel.isError ? <Text style={styles.error}>{message(cancel.error)}</Text> : null}
                <Button title="Cancel schedule" variant="secondary" onPress={() => cancel.mutate()} loading={cancel.isPending} />
              </Card>
            ) : null}
          </>
        )}
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
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  metric: { minWidth: 120, gap: theme.space(1) },
  emi: { borderTopWidth: 1, borderTopColor: theme.color.border, paddingTop: theme.space(2), gap: theme.space(2) },
  emiTitle: { color: theme.color.text, fontSize: theme.font.body, fontWeight: "700" },
  actions: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

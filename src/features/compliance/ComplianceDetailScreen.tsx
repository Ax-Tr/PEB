import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import type { StackScreenProps } from "@react-navigation/stack";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { QueryState } from "../../components/QueryState";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import type { ComplianceReport, ComplianceReportLine } from "../../shared/types";
import { theme } from "../../theme/theme";
import type { BooksStackParamList } from "../../navigation/types";
import {
  useApproveReport,
  useRecordFiling,
  useReport,
  useReportLines,
  useReviewReport,
  useSetReconciled,
} from "./hooks";
import { StatusPill } from "./ComplianceListScreen";

type Props = StackScreenProps<BooksStackParamList, "ComplianceDetail">;

/** Report lifecycle: reconcile → review → approve → record filing acknowledgement. */
export function ComplianceDetailScreen({ route }: Props): React.ReactElement {
  const { reportId } = route.params;
  const report = useReport(reportId);
  const lines = useReportLines(reportId);

  const setReconciled = useSetReconciled();
  const review = useReviewReport();
  const approve = useApproveReport();
  const filing = useRecordFiling();
  const [ack, setAck] = useState("");

  const busyErr =
    (setReconciled.error ?? review.error ?? approve.error ?? filing.error) instanceof ApiError
      ? ((setReconciled.error ?? review.error ?? approve.error ?? filing.error) as ApiError).message
      : null;

  return (
    <Screen>
      <QueryState query={report}>
        {(r: ComplianceReport) => (
          <>
            <Card>
              <View style={styles.row}>
                <Text style={styles.title}>{r.type.replace(/_/g, " ")}</Text>
                <StatusPill state={r.displayState} />
              </View>
              <View style={styles.line}>
                <Text style={styles.label}>Taxable</Text>
                <Money minor={r.totalTaxableMinor} size="body" />
              </View>
              <View style={styles.line}>
                <Text style={styles.label}>Tax</Text>
                <Money minor={r.totalTaxMinor} size="body" />
              </View>
              <View style={styles.line}>
                <Text style={styles.label}>Net payable</Text>
                <Money minor={r.netPayableMinor} size="title" />
              </View>
              {r.missingFields.length > 0 ? (
                <View style={styles.flags}>
                  {r.missingFields.map((f, i) => (
                    <Text key={i} style={styles.flag}>⚠ {f}</Text>
                  ))}
                </View>
              ) : null}
            </Card>

            <Card title="Report lines">
              <QueryState query={lines} emptyWhen={(l) => l.length === 0} emptyText="No lines.">
                {(ls: ComplianceReportLine[]) => (
                  <View>
                    {ls.map((l) => (
                      <View key={l.id} style={styles.line}>
                        <Text style={styles.label}>{l.label}</Text>
                        <Money minor={l.amountMinor} size="body" />
                      </View>
                    ))}
                  </View>
                )}
              </QueryState>
            </Card>

            <Card title="Workflow">
              {busyErr ? <Text style={styles.error}>{busyErr}</Text> : null}
              <View style={styles.line}>
                <Text style={styles.label}>Data reconciled</Text>
                <Button
                  title={r.dataReconciled ? "Reconciled ✓" : "Mark reconciled"}
                  variant={r.dataReconciled ? "secondary" : "primary"}
                  onPress={() => setReconciled.mutate({ id: r.id, body: { reconciled: !r.dataReconciled } })}
                  loading={setReconciled.isPending}
                />
              </View>

              {r.status === "DRAFT" ? (
                <Button title="Submit for review" onPress={() => review.mutate({ id: r.id })} loading={review.isPending} />
              ) : null}

              {r.status === "REVIEWED" ? (
                <Button
                  title="Approve"
                  onPress={() => approve.mutate({ id: r.id })}
                  loading={approve.isPending}
                  disabled={!r.dataReconciled}
                />
              ) : null}
              {r.status === "REVIEWED" && !r.dataReconciled ? (
                <Text style={styles.hint}>Approval is blocked until the data is reconciled.</Text>
              ) : null}

              {r.status === "APPROVED" ? (
                <>
                  <TextField
                    label="Official acknowledgement reference"
                    value={ack}
                    onChangeText={setAck}
                    placeholder="ARN / portal ack no."
                  />
                  <Button
                    title="Record filing acknowledgement"
                    onPress={() => filing.mutate({ id: r.id, body: { ackReference: ack.trim() } })}
                    loading={filing.isPending}
                    disabled={ack.trim().length === 0}
                  />
                  <Text style={styles.hint}>
                    This records an external acknowledgement only — it does not file with the tax portal.
                  </Text>
                </>
              ) : null}

              {r.status === "FILED" ? (
                <Text style={styles.done}>Filed · ack {r.ackReference}</Text>
              ) : null}
            </Card>
          </>
        )}
      </QueryState>
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  title: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "800" },
  line: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: theme.space(1) },
  label: { color: theme.color.textMuted, fontSize: theme.font.body, flexShrink: 1, paddingRight: theme.space(2) },
  flags: { gap: theme.space(1), marginTop: theme.space(2) },
  flag: { color: theme.color.warning, fontSize: theme.font.caption },
  hint: { color: theme.color.textMuted, fontSize: theme.font.caption },
  done: { color: theme.color.success, fontSize: theme.font.body, fontWeight: "700" },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

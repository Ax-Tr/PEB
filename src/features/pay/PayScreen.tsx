import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { EntityPicker } from "../../components/EntityPicker";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import { parseRupeesToMinor } from "../../shared/money";
import { STEP_UP_THRESHOLD_MINOR, requestStepUp } from "../../shared/stepUp";
import type { BankAccount, CreatePayoutResponse, Vendor } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useVendorBankAccounts, useVendors } from "../masters/hooks";
import { useApprovePayout, useCreatePayout, useRejectPayout } from "./hooks";

/** Pay a vendor/employee: create a payout (maker) and, if high-risk, route to approval (checker). */
export function PayScreen(): React.ReactElement {
  const vendors = useVendors();
  const [vendor, setVendor] = useState<Vendor | null>(null);
  const [beneficiaryId, setBeneficiaryId] = useState<string | null>(null);
  const bankAccounts = useVendorBankAccounts(vendor?.id ?? null);
  const [amount, setAmount] = useState("");
  const [purpose, setPurpose] = useState("");
  const [amountError, setAmountError] = useState<string | undefined>();

  const create = useCreatePayout();
  const approve = useApprovePayout();
  const reject = useRejectPayout();
  const result: CreatePayoutResponse | undefined = create.data;

  const [stepUpNote, setStepUpNote] = useState<string | null>(null);

  const onCreate = async () => {
    setAmountError(undefined);
    setStepUpNote(null);
    let amountMinor: number;
    try {
      amountMinor = parseRupeesToMinor(amount);
    } catch {
      setAmountError("Enter a valid amount, e.g. 5000.00");
      return;
    }
    if (amountMinor <= 0) {
      setAmountError("Amount must be greater than zero");
      return;
    }
    // High-value payouts require biometric step-up before creation.
    let stepUpVerified = false;
    if (amountMinor >= STEP_UP_THRESHOLD_MINOR) {
      stepUpVerified = await requestStepUp("Confirm this high-value payout");
      if (!stepUpVerified) {
        setStepUpNote("Step-up verification is required for this amount. Please authenticate to proceed.");
        return;
      }
    }
    if (!vendor || !beneficiaryId) {
      return;
    }
    create.mutate({
      partyType: "VENDOR",
      partyId: vendor.id,
      beneficiaryId,
      amountMinor,
      purpose: purpose.trim() || undefined,
      stepUpVerified,
    });
  };

  return (
    <Screen>
      <Text style={styles.title}>Pay a vendor</Text>

      {!result ? (
        <Card>
          <EntityPicker<Vendor>
            testID="pay-vendor"
            label="Vendor"
            items={vendors.data}
            loading={vendors.isLoading}
            selectedId={vendor?.id ?? null}
            getId={(v) => v.id}
            getLabel={(v) => v.name}
            onSelect={(v) => {
              setVendor(v);
              setBeneficiaryId(null);
            }}
            emptyText="No vendors yet."
          />
          <EntityPicker<BankAccount>
            testID="pay-beneficiary"
            label="Beneficiary bank account"
            items={bankAccounts.data}
            loading={bankAccounts.isLoading}
            selectedId={beneficiaryId}
            getId={(b) => b.id}
            getLabel={(b) => b.accountNumberMasked}
            onSelect={(b) => setBeneficiaryId(b.id)}
            emptyText={vendor ? "No confirmed bank accounts for this vendor." : "Select a vendor first."}
          />
          <TextField label="Amount (₹)" value={amount} onChangeText={setAmount} placeholder="0.00" keyboardType="decimal-pad" error={amountError} />
          <TextField label="Purpose (optional)" value={purpose} onChangeText={setPurpose} placeholder="e.g. June supplies" />
          {create.isError ? (
            <Text style={styles.error}>
              {create.error instanceof ApiError ? create.error.message : "Could not create the payout."}
            </Text>
          ) : null}
          {stepUpNote ? <Text style={styles.error}>{stepUpNote}</Text> : null}
          <Button
            title="Create payout"
            onPress={onCreate}
            loading={create.isPending}
            disabled={!vendor || !beneficiaryId || amount.length === 0}
          />
        </Card>
      ) : (
        <Card title="Payout created">
          <View style={styles.rowBetween}>
            <Text style={styles.status}>{result.status}</Text>
            <RiskBadge risk={result.riskLevel} />
          </View>
          {result.requiresApproval ? (
            <>
              <Text style={styles.sub}>
                This payout needs a second approver (maker-checker). Approve or reject below — the maker
                cannot approve their own.
              </Text>
              {approve.isError || reject.isError ? (
                <Text style={styles.error}>{actionErrorMessage(approve.error ?? reject.error)}</Text>
              ) : null}
              {approve.data || reject.data ? (
                <Text style={styles.done}>Now: {(approve.data ?? reject.data)?.status}</Text>
              ) : (
                <View style={styles.actions}>
                  <Button title="Approve" onPress={() => approve.mutate(result.payoutId)} loading={approve.isPending} />
                  <Button
                    title="Reject"
                    variant="secondary"
                    onPress={() => reject.mutate({ id: result.payoutId, reason: "Rejected by approver" })}
                    loading={reject.isPending}
                  />
                </View>
              )}
            </>
          ) : (
            <Text style={styles.sub}>Low-risk payout — queued for processing.</Text>
          )}
          <Button
            title="New payout"
            variant="secondary"
            onPress={() => {
              create.reset();
              approve.reset();
              reject.reset();
              setVendor(null);
              setBeneficiaryId(null);
              setAmount("");
              setPurpose("");
            }}
          />
        </Card>
      )}
    </Screen>
  );
}

function actionErrorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : "Action failed.";
}

function RiskBadge({ risk }: { risk: string }): React.ReactElement {
  const color = risk === "HIGH" ? theme.color.danger : risk === "MEDIUM" ? theme.color.warning : theme.color.success;
  return (
    <View style={[styles.badge, { borderColor: color }]}>
      <Text style={[styles.badgeText, { color }]}>{risk} risk</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  status: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.body },
  done: { color: theme.color.success, fontSize: theme.font.body, fontWeight: "700" },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
  actions: { flexDirection: "row", gap: theme.space(3) },
  rowBetween: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  badge: { borderWidth: 1, borderRadius: theme.radius.pill, paddingHorizontal: theme.space(2), paddingVertical: theme.space(1) },
  badgeText: { fontSize: theme.font.caption, fontWeight: "700" },
});

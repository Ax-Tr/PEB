import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import QRCode from "react-native-qrcode-svg";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import { parseRupeesToMinor } from "../../shared/money";
import { theme } from "../../theme/theme";
import { useCreatePaymentRequest, useSimulatePayment } from "./hooks";

/** Collect money: enter an amount, generate a UPI payment request + QR for the customer to scan. */
export function ReceiveScreen(): React.ReactElement {
  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const [amountError, setAmountError] = useState<string | undefined>();
  const create = useCreatePaymentRequest();
  const simulate = useSimulatePayment();

  const onCreate = () => {
    setAmountError(undefined);
    let amountMinor: number;
    try {
      amountMinor = parseRupeesToMinor(amount);
    } catch {
      setAmountError("Enter a valid amount, e.g. 1499.00");
      return;
    }
    if (amountMinor <= 0) {
      setAmountError("Amount must be greater than zero");
      return;
    }
    create.mutate({ amountMinor, reference: reference.trim() || "Payment" });
  };

  const result = create.data;

  return (
    <Screen>
      <Text style={styles.title}>Receive payment</Text>

      {!result ? (
        <Card>
          <TextField
            testID="receive-amount"
            label="Amount (₹)"
            value={amount}
            onChangeText={setAmount}
            placeholder="0.00"
            keyboardType="decimal-pad"
            error={amountError}
            autoFocus
          />
          <TextField label="Reference (optional)" value={reference} onChangeText={setReference} placeholder="e.g. Invoice #42" />
          {create.isError ? (
            <Text style={styles.error}>
              {create.error instanceof ApiError ? create.error.message : "Could not create the request."}
            </Text>
          ) : null}
          <Button testID="receive-generate" title="Generate QR" onPress={onCreate} loading={create.isPending} disabled={amount.length === 0} />
        </Card>
      ) : (
        <Card title="Show this to your customer">
          <View style={styles.qrWrap}>
            {result.upiUri ? (
              <QRCode value={result.upiUri} size={220} backgroundColor="#FFFFFF" />
            ) : (
              <Text style={styles.error}>No UPI URI returned for this request.</Text>
            )}
          </View>
          <Money minor={result.amountMinor} size="h1" />
          <Text style={styles.sub}>Ref: {result.reference} · Status: {result.status}</Text>
          <Button
            title="Received"
            loading={simulate.isPending}
            onPress={() => {
              if (result.requestId) {
                simulate.mutate(result.requestId, {
                  onSuccess: () => {
                    alert("Payment received successfully!");
                    create.reset();
                    setAmount("");
                    setReference("");
                  },
                  onError: (err) => {
                    alert("Failed to mark as received");
                  }
                });
              }
            }}
          />
          <Button
            title="New request"
            variant="secondary"
            onPress={() => {
              create.reset();
              setAmount("");
              setReference("");
            }}
          />
        </Card>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  qrWrap: { alignItems: "center", backgroundColor: "#FFFFFF", borderRadius: theme.radius.md, padding: theme.space(4) },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

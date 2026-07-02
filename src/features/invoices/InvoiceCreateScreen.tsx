import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Money } from "../../components/Money";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { ApiError } from "../../shared/http";
import { parseRupeesToMinor } from "../../shared/money";
import type { InvoiceResponse } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useCreateInvoice, useSendInvoice } from "./hooks";

/** Create a GST invoice (single line for now) — tax is computed server-side and shown on the result. */
export function InvoiceCreateScreen(): React.ReactElement {
  const [customerName, setCustomerName] = useState("");
  const [supplyType, setSupplyType] = useState<"B2B" | "B2C">("B2C");
  const [description, setDescription] = useState("");
  const [unitPrice, setUnitPrice] = useState("");
  const [qty, setQty] = useState("1");
  const [gstRate, setGstRate] = useState("18");
  const [err, setErr] = useState<string | undefined>();

  const create = useCreateInvoice();
  const send = useSendInvoice();
  const invoice: InvoiceResponse | undefined = create.data;

  const onCreate = () => {
    setErr(undefined);
    let unitPriceMinor: number;
    try {
      unitPriceMinor = parseRupeesToMinor(unitPrice);
    } catch {
      setErr("Enter a valid unit price, e.g. 1000.00");
      return;
    }
    const quantity = Number(qty);
    const rate = Number(gstRate);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setErr("Quantity must be greater than zero");
      return;
    }
    create.mutate({
      documentType: "INVOICE",
      supplyType,
      customerName: customerName.trim() || undefined,
      reverseCharge: false,
      lines: [
        {
          description: description.trim() || "Item",
          quantity,
          unitPriceMinor,
          discountMinor: 0,
          gstRate: rate,
        },
      ],
    });
  };

  return (
    <Screen>
      <Text style={styles.title}>New invoice</Text>
      {!invoice ? (
        <Card>
          <TextField label="Customer name" value={customerName} onChangeText={setCustomerName} placeholder="e.g. Acme Traders" autoFocus />
          <View style={styles.toggleRow}>
            <Button title="B2C" variant={supplyType === "B2C" ? "primary" : "secondary"} onPress={() => setSupplyType("B2C")} />
            <Button title="B2B" variant={supplyType === "B2B" ? "primary" : "secondary"} onPress={() => setSupplyType("B2B")} />
          </View>
          <TextField label="Item description" value={description} onChangeText={setDescription} placeholder="e.g. Consulting" />
          <TextField label="Unit price (₹)" value={unitPrice} onChangeText={setUnitPrice} placeholder="0.00" keyboardType="decimal-pad" error={err} />
          <TextField label="Quantity" value={qty} onChangeText={setQty} keyboardType="decimal-pad" />
          <TextField label="GST rate (%)" value={gstRate} onChangeText={setGstRate} keyboardType="number-pad" />
          {create.isError ? (
            <Text style={styles.error}>{create.error instanceof ApiError ? create.error.message : "Could not create invoice."}</Text>
          ) : null}
          <Button title="Create invoice" onPress={onCreate} loading={create.isPending} disabled={unitPrice.length === 0} />
        </Card>
      ) : (
        <Card title={`Invoice ${invoice.invoiceNumber}`}>
          <View style={styles.line}>
            <Text style={styles.label}>Taxable</Text>
            <Money minor={invoice.totalTaxableMinor} size="body" />
          </View>
          <View style={styles.line}>
            <Text style={styles.label}>GST</Text>
            <Money minor={invoice.totalTaxMinor} size="body" />
          </View>
          <View style={styles.line}>
            <Text style={styles.label}>Total</Text>
            <Money minor={invoice.totalAmountMinor} size="title" />
          </View>
          <Text style={styles.sub}>Status: {send.data?.status ?? invoice.status}</Text>
          {send.isError ? (
            <Text style={styles.error}>{send.error instanceof ApiError ? send.error.message : "Send failed."}</Text>
          ) : null}
          {!send.data ? (
            <Button title="Send to customer" onPress={() => send.mutate({ id: invoice.id, channel: "SMS" })} loading={send.isPending} />
          ) : (
            <Text style={styles.done}>Sent via SMS</Text>
          )}
          <Button
            title="New invoice"
            variant="secondary"
            onPress={() => {
              create.reset();
              send.reset();
              setDescription("");
              setUnitPrice("");
            }}
          />
        </Card>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  toggleRow: { flexDirection: "row", gap: theme.space(3) },
  line: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  label: { color: theme.color.textMuted, fontSize: theme.font.body },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  done: { color: theme.color.success, fontWeight: "700" },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
});

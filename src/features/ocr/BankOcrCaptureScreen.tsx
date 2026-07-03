import React, { useEffect, useMemo, useState } from "react";
import { ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { Button } from "../../components/Button";
import { EntityPicker } from "../../components/EntityPicker";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import type { OcrExtractedField, Vendor } from "../../shared/types";
import { theme } from "../../theme/theme";
import { useAddVendorBankAccount, useVendors } from "../masters/hooks";
import { useReserveOcrUpload, useReviewOcrJob, useStartOcrJob } from "./hooks";

const FIELD_LABELS: Record<string, string> = {
  accountNumber: "Account number",
  ifsc: "IFSC",
  upi: "UPI",
  bankName: "Bank name",
  holderName: "Holder name",
};
const REQUIRED_BANK_FIELDS = ["accountNumber", "ifsc", "bankName", "holderName"];

export function BankOcrCaptureScreen(): React.ReactElement {
  const vendors = useVendors();
  const reserveUpload = useReserveOcrUpload();
  const startJob = useStartOcrJob();
  const reviewJob = useReviewOcrJob();

  const [vendor, setVendor] = useState<Vendor | null>(null);
  const [filename, setFilename] = useState("bank-proof.pdf");
  const [rawText, setRawText] = useState(
    "HDFC Bank\nAccount No: 50100123456789\nIFSC: HDFC0001234\nHolder: RAHUL SHARMA",
  );
  const [reviewValues, setReviewValues] = useState<Record<string, string>>({});
  const addBankAccount = useAddVendorBankAccount(vendor?.id ?? null);

  const job = reviewJob.data ?? startJob.data;
  const fields = job?.fields ?? {};
  const visibleFieldKeys = useMemo(
    () => Array.from(new Set([...REQUIRED_BANK_FIELDS, "upi", ...Object.keys(fields)])),
    [fields],
  );
  const validationMessage = validateBankFields(reviewValues);
  const canAccept = Boolean(job) && job?.status === "REVIEW_REQUIRED" && !validationMessage;
  const canSave = Boolean(vendor) && job?.status === "COMPLETED" && !validationMessage;

  useEffect(() => {
    setReviewValues(flatten(fields));
  }, [fields, job?.id]);

  const extract = async () => {
    const reservation = await reserveUpload.mutateAsync({
      filename,
      mimeType: filename.toLowerCase().endsWith(".png")
        ? "image/png"
        : filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")
          ? "image/jpeg"
          : "application/pdf",
      sizeBytes: Math.max(1, rawText.length),
    });
    await startJob.mutateAsync({
      documentId: reservation.documentId,
      documentType: "BANK_DETAILS",
      rawText,
    });
  };

  const accept = async () => {
    if (!job || !canAccept) return;
    await reviewJob.mutateAsync({
      id: job.id,
      accepted: true,
      fields: reviewValues,
    });
  };

  const saveToVendor = async () => {
    if (!vendor || !canSave) return;
    await addBankAccount.mutateAsync({
      accountNumber: reviewValues.accountNumber,
      ifsc: reviewValues.ifsc,
      upi: reviewValues.upi,
      bankName: reviewValues.bankName,
      holderName: reviewValues.holderName,
      source: "OCR",
    });
  };

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>OCR bank capture</Text>
        <EntityPicker
          label="Vendor"
          items={vendors.data}
          loading={vendors.isLoading}
          selectedId={vendor?.id ?? null}
          getId={(item) => item.id}
          getLabel={(item) => item.name}
          onSelect={setVendor}
          emptyText="No vendors found"
        />
        <TextField label="File name" value={filename} onChangeText={setFilename} />
        <View style={styles.rawWrap}>
          <Text style={styles.label}>OCR text</Text>
          <TextInput
            accessibilityLabel="OCR text"
            value={rawText}
            onChangeText={setRawText}
            multiline
            textAlignVertical="top"
            style={styles.rawInput}
          />
        </View>
        <Button
          title="Extract bank details"
          onPress={extract}
          loading={reserveUpload.isPending || startJob.isPending}
          disabled={rawText.trim().length < 10 || filename.trim().length === 0}
        />

        {job ? (
          <View style={styles.result}>
            <Text style={styles.sectionTitle}>
              {job.status} - confidence {Number(job.confidence).toFixed(2)}
            </Text>
            {visibleFieldKeys.map((key) => (
              <View key={key} style={styles.fieldRow}>
                <Text style={styles.fieldName}>{FIELD_LABELS[key] ?? key}</Text>
                <TextInput
                  value={reviewValues[key] ?? ""}
                  onChangeText={(value) => setReviewValues((prev) => ({ ...prev, [key]: value }))}
                  editable={job.status === "REVIEW_REQUIRED"}
                  autoCapitalize={key === "ifsc" ? "characters" : "words"}
                  style={styles.reviewInput}
                />
                {fields[key]?.confidence !== undefined ? (
                  <Text style={styles.confidence}>
                    OCR confidence {Math.round(fields[key].confidence * 100)}%
                  </Text>
                ) : null}
              </View>
            ))}
            {validationMessage ? <Text style={styles.error}>{validationMessage}</Text> : null}
            <View style={styles.actions}>
              <Button
                title="Accept OCR"
                onPress={accept}
                loading={reviewJob.isPending}
                disabled={!canAccept}
                variant="secondary"
              />
              <Button
                title="Save to vendor review"
                onPress={saveToVendor}
                loading={addBankAccount.isPending}
                disabled={!canSave}
              />
            </View>
            {addBankAccount.data ? (
              <Text style={styles.success}>
                Saved as {addBankAccount.data.status ?? "PENDING_REVIEW"} for {vendor?.name}.
              </Text>
            ) : null}
          </View>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

function flatten(fields: Record<string, OcrExtractedField>): Record<string, string> {
  return Object.fromEntries(Object.entries(fields).map(([key, field]) => [key, field.value ?? ""]));
}

function validateBankFields(values: Record<string, string>): string | null {
  for (const key of REQUIRED_BANK_FIELDS) {
    if (!values[key]?.trim()) {
      return `${FIELD_LABELS[key]} is required before accepting OCR.`;
    }
  }
  const accountNumber = values.accountNumber.replace(/[\s-]/g, "");
  if (!/^\d{6,18}$/.test(accountNumber)) {
    return "Account number must be 6 to 18 digits.";
  }
  if (!/^[A-Z]{4}0[A-Z0-9]{6}$/.test(values.ifsc.trim().toUpperCase())) {
    return "IFSC must use the standard 11-character format.";
  }
  return null;
}

const styles = StyleSheet.create({
  content: { gap: theme.space(4), paddingBottom: theme.space(6) },
  title: { color: theme.color.text, fontSize: theme.font.h2, fontWeight: "800" },
  rawWrap: { gap: theme.space(1) },
  label: { color: theme.color.textMuted, fontSize: theme.font.caption },
  rawInput: {
    minHeight: 136,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.color.border,
    backgroundColor: theme.color.surface,
    color: theme.color.text,
    padding: theme.space(3),
    fontSize: theme.font.body,
  },
  result: {
    borderWidth: 1,
    borderColor: theme.color.border,
    borderRadius: theme.radius.md,
    backgroundColor: theme.color.surface,
    padding: theme.space(3),
    gap: theme.space(3),
  },
  sectionTitle: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "700" },
  fieldRow: { gap: theme.space(1) },
  fieldName: { color: theme.color.textMuted, fontSize: theme.font.caption },
  reviewInput: {
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.color.border,
    backgroundColor: theme.color.bg,
    color: theme.color.text,
    padding: theme.space(2),
    fontSize: theme.font.body,
  },
  confidence: { color: theme.color.textMuted, fontSize: theme.font.caption },
  actions: { gap: theme.space(2) },
  error: { color: theme.color.danger, fontSize: theme.font.caption },
  success: { color: theme.color.success, fontSize: theme.font.caption },
});

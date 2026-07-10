import React, { useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { TextField } from "../../components/TextField";
import { auth, http } from "../../shared/api";
import { API_PREFIX } from "../../shared/config";
import { ApiError } from "../../shared/http";
import { theme } from "../../theme/theme";
import { useAuth } from "../auth/AuthContext";

const BUSINESS_TYPES: { value: string; label: string }[] = [
  { value: "PROPRIETOR",   label: "Sole Proprietorship" },
  { value: "PARTNERSHIP",  label: "Partnership" },
  { value: "PVT_LTD",      label: "Private Limited" },
  { value: "LLP",          label: "LLP" },
  { value: "OTHER",        label: "Other" },
];
const STATE_CODES: { label: string; code: string }[] = [
  { label: "Andhra Pradesh", code: "37" },
  { label: "Arunachal Pradesh", code: "12" },
  { label: "Assam", code: "18" },
  { label: "Bihar", code: "10" },
  { label: "Chhattisgarh", code: "22" },
  { label: "Delhi", code: "07" },
  { label: "Goa", code: "30" },
  { label: "Gujarat", code: "24" },
  { label: "Haryana", code: "06" },
  { label: "Himachal Pradesh", code: "02" },
  { label: "Jharkhand", code: "20" },
  { label: "Karnataka", code: "29" },
  { label: "Kerala", code: "32" },
  { label: "Madhya Pradesh", code: "23" },
  { label: "Maharashtra", code: "27" },
  { label: "Manipur", code: "14" },
  { label: "Meghalaya", code: "17" },
  { label: "Mizoram", code: "15" },
  { label: "Nagaland", code: "13" },
  { label: "Odisha", code: "21" },
  { label: "Punjab", code: "03" },
  { label: "Rajasthan", code: "08" },
  { label: "Sikkim", code: "11" },
  { label: "Tamil Nadu", code: "33" },
  { label: "Telangana", code: "36" },
  { label: "Tripura", code: "16" },
  { label: "Uttar Pradesh", code: "09" },
  { label: "Uttarakhand", code: "05" },
  { label: "West Bengal", code: "19" },
];

interface CreateBusinessResponse {
  id: string;
  ownerUserId: string;
  legalName: string;
  status: string;
}

/** Shown when authed but tenantId is null. Guides the user through creating their first business. */
export function BusinessOnboardingScreen(): React.ReactElement {
  const { refreshTenant } = useAuth();
  const [legalName, setLegalName] = useState("");
  const [tradeName, setTradeName] = useState("");
  const [businessType, setBusinessType] = useState(BUSINESS_TYPES[0].value);
  const [stateCode, setStateCode] = useState(STATE_CODES[0].code);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async () => {
    setBusy(true);
    setError(null);
    try {
      const res = await http.post<CreateBusinessResponse>(`${API_PREFIX}/businesses`, {
        legalName: legalName.trim(),
        tradeName: tradeName.trim() || undefined,
        businessType,
        stateCode,
      });

      // 1. Tell identity-service to attach this tenant to the user (so the next JWT has tenant_id)
      await http.post(`${API_PREFIX}/auth/link-tenant`, { tenantId: res.id }, { idempotencyKey: res.id });

      // 2. Refresh tokens — the new access token will now include tenant_id
      await auth.refresh();

      // 3. Persist tenantId locally and re-gate the navigator
      auth.setTenantId(res.id);
      await refreshTenant();
    } catch (e) {
      if (e instanceof ApiError) {
        setError(`Error ${e.status}: ${e.message}`);
      } else if (e instanceof Error) {
        setError(`Error: ${e.message}`);
      } else {
        setError("Failed to create business. Please try again.");
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView style={styles.bg} contentContainerStyle={styles.container}>
      {/* Hero */}
      <View style={styles.hero}>
        <Text style={styles.emoji}>🏪</Text>
        <Text style={styles.heroTitle}>Set up your business</Text>
        <Text style={styles.heroSub}>
          Just a few details to get your account ready. You can update everything later.
        </Text>
      </View>

      <Card title="Business details">
        {/* Legal Name */}
        <TextField
          testID="onboard-legal-name"
          label="Legal business name *"
          value={legalName}
          onChangeText={setLegalName}
          placeholder="e.g. Sharma Traders"
          autoFocus
        />

        {/* Trade Name */}
        <TextField
          testID="onboard-trade-name"
          label="Trade name (optional)"
          value={tradeName}
          onChangeText={setTradeName}
          placeholder="e.g. Sharma Electronics"
        />

        {/* Business Type */}
        <Text style={styles.label}>Business type *</Text>
        <View style={styles.chipRow}>
          {BUSINESS_TYPES.map((t) => (
            <TypeChip
              key={t.value}
              label={t.label}
              selected={businessType === t.value}
              onPress={() => setBusinessType(t.value)}
            />
          ))}
        </View>

        {/* State */}
        <Text style={styles.label}>State (GST state) *</Text>
        <View style={styles.chipRow}>
          {STATE_CODES.slice(0, 10).map((s) => (
            <TypeChip
              key={s.code}
              label={s.label}
              selected={stateCode === s.code}
              onPress={() => setStateCode(s.code)}
            />
          ))}
        </View>
        <View style={styles.chipRow}>
          {STATE_CODES.slice(10).map((s) => (
            <TypeChip
              key={s.code}
              label={s.label}
              selected={stateCode === s.code}
              onPress={() => setStateCode(s.code)}
            />
          ))}
        </View>

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Button
          testID="onboard-submit"
          title="Create business"
          onPress={() => void onSubmit()}
          loading={busy}
          disabled={legalName.trim().length < 2}
        />
      </Card>

      <Text style={styles.fine}>
        By continuing you agree to PayWithEase's Terms of Service and Privacy Policy.
      </Text>
    </ScrollView>
  );
}

function TypeChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}): React.ReactElement {
  return (
    <Text
      onPress={onPress}
      style={[styles.chip, selected && styles.chipSelected]}
      accessibilityRole="button"
    >
      {label}
    </Text>
  );
}

const styles = StyleSheet.create({
  bg: { flex: 1, backgroundColor: theme.color.bg },
  container: { padding: theme.space(4), gap: theme.space(4) },
  hero: { alignItems: "center", gap: theme.space(2), paddingVertical: theme.space(6) },
  emoji: { fontSize: 48 },
  heroTitle: {
    color: theme.color.text,
    fontSize: theme.font.h1,
    fontWeight: "800",
    textAlign: "center",
  },
  heroSub: {
    color: theme.color.textMuted,
    fontSize: theme.font.body,
    textAlign: "center",
    maxWidth: 320,
  },
  label: {
    color: theme.color.textMuted,
    fontSize: theme.font.caption,
    fontWeight: "600",
    marginTop: theme.space(3),
    marginBottom: theme.space(1),
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  chipRow: { flexDirection: "row", flexWrap: "wrap", gap: theme.space(2) },
  chip: {
    paddingHorizontal: theme.space(3),
    paddingVertical: theme.space(1),
    borderRadius: theme.radius.pill,
    borderWidth: 1,
    borderColor: theme.color.border,
    color: theme.color.textMuted,
    fontSize: theme.font.caption,
  },
  chipSelected: {
    borderColor: theme.color.primary,
    color: theme.color.primary,
    fontWeight: "700",
  },
  error: {
    color: theme.color.danger,
    fontSize: theme.font.caption,
    marginVertical: theme.space(2),
  },
  fine: {
    color: theme.color.textMuted,
    fontSize: theme.font.caption,
    textAlign: "center",
    paddingBottom: theme.space(8),
  },
});

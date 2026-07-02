import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Screen } from "../../components/Screen";
import { TextField } from "../../components/TextField";
import { auth } from "../../shared/api";
import { ApiError } from "../../shared/http";
import { theme } from "../../theme/theme";
import { useI18n } from "../i18n/I18nContext";
import { useAuth } from "./AuthContext";

type Step = "phone" | "otp";

/** Phone + OTP login against identity-service. Errors surface the RFC-7807 detail from the API. */
export function LoginScreen(): React.ReactElement {
  const { refreshAuthed } = useAuth();
  const { t } = useI18n();
  const [step, setStep] = useState<Step>("phone");
  const [mobile, setMobile] = useState("");
  const [code, setCode] = useState("");
  const [challengeId, setChallengeId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onRequest = async () => {
    setBusy(true);
    setError(null);
    try {
      const res = await auth.requestOtp(mobile.trim());
      setChallengeId(res.challengeId);
      setStep("otp");
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  };

  const onVerify = async () => {
    setBusy(true);
    setError(null);
    try {
      await auth.verifyOtp(mobile.trim(), code.trim(), challengeId);
      await refreshAuthed();
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Screen>
      <View style={styles.hero}>
        <Text style={styles.brand}>PayWithEase</Text>
        <Text style={styles.tagline}>{t("app.tagline")}</Text>
      </View>
      <Card title={step === "phone" ? t("auth.signIn") : t("auth.enterOtp")}>
        {step === "phone" ? (
          <>
            <TextField
              testID="login-mobile"
              label={t("auth.mobile")}
              value={mobile}
              onChangeText={setMobile}
              placeholder="10-digit mobile"
              keyboardType="phone-pad"
              maxLength={13}
              autoFocus
              error={error ?? undefined}
            />
            <Button testID="login-send-otp" title={t("auth.sendOtp")} onPress={onRequest} loading={busy} disabled={mobile.length < 10} />
          </>
        ) : (
          <>
            <TextField
              testID="login-otp"
              label={t("auth.otpSentTo", { mobile })}
              value={code}
              onChangeText={setCode}
              placeholder="6-digit code"
              keyboardType="number-pad"
              maxLength={6}
              autoFocus
              error={error ?? undefined}
            />
            <Button testID="login-verify" title={t("auth.verify")} onPress={onVerify} loading={busy} disabled={code.length < 4} />
            <Button title={t("auth.changeNumber")} variant="secondary" onPress={() => setStep("phone")} />
          </>
        )}
      </Card>
    </Screen>
  );
}

function messageOf(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return "Network error. Please check your connection and try again.";
}

const styles = StyleSheet.create({
  hero: { alignItems: "center", gap: theme.space(1), paddingVertical: theme.space(8) },
  brand: { color: theme.color.text, fontSize: theme.font.h1, fontWeight: "800" },
  tagline: { color: theme.color.textMuted, fontSize: theme.font.body },
});

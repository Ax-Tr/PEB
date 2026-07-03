import React, { useState } from "react";
import { Modal, StyleSheet, Text, TextInput, View } from "react-native";
import { Button } from "../../components/Button";
import { theme } from "../../theme/theme";

interface Props {
  visible: boolean;
  loading?: boolean;
  onClose: () => void;
  onSubmit: (transcript: string) => void;
}

export function VoiceCaptureModal({ visible, loading = false, onClose, onSubmit }: Props): React.ReactElement {
  const [transcript, setTranscript] = useState("Raj promised to pay 5000 on Friday");
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <Text style={styles.title}>Voice financial input</Text>
          <Text style={styles.sub}>Paste or type the transcript. Native speech-to-text can plug into this modal later.</Text>
          <TextInput
            accessibilityLabel="Voice transcript"
            value={transcript}
            onChangeText={setTranscript}
            multiline
            textAlignVertical="top"
            style={styles.input}
          />
          <View style={styles.actions}>
            <Button title="Cancel" variant="secondary" onPress={onClose} />
            <Button
              title="Create draft"
              onPress={() => onSubmit(transcript.trim())}
              loading={loading}
              disabled={transcript.trim().length < 5}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: "center", padding: theme.space(4), backgroundColor: "rgba(0,0,0,0.55)" },
  sheet: { backgroundColor: theme.color.surface, borderRadius: theme.radius.md, borderWidth: 1, borderColor: theme.color.border, padding: theme.space(4), gap: theme.space(3) },
  title: { color: theme.color.text, fontSize: theme.font.title, fontWeight: "800" },
  sub: { color: theme.color.textMuted, fontSize: theme.font.caption },
  input: { minHeight: 120, borderRadius: theme.radius.md, borderWidth: 1, borderColor: theme.color.border, color: theme.color.text, padding: theme.space(3) },
  actions: { flexDirection: "row", gap: theme.space(3), justifyContent: "flex-end" },
});

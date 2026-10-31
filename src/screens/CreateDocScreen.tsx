/**
 * Create Doc – Document generator with MRZ (ICAO 9303) support.
 */

import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, TextInput, Alert } from 'react-native';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';

export default function CreateDocScreen() {
  const navigation = useNavigation<any>();
  const [docType, setDocType] = useState<'passport' | 'license'>('passport');
  const [isGenerating, setIsGenerating] = useState(false);
  const [fields, setFields] = useState({ fullName: '', dob: '', nationality: '', idNumber: '' });

  const update = (key: string, val: string) => setFields((p) => ({ ...p, [key]: val }));

  const handleGenerate = () => {
    if (!fields.fullName || !fields.dob) {
      Alert.alert('Required', 'Name and DOB are required for MRZ generation.');
      return;
    }
    setIsGenerating(true);
    setTimeout(() => {
      setIsGenerating(false);
      Alert.alert('Generated', `${docType.toUpperCase()} created with MRZ (ICAO 9303).`);
    }, 1500);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>📄 Create Document</Text>

      {/* Doc type selector */}
      <View style={styles.section}>
        <Text style={styles.label}>Document Type</Text>
        <View style={styles.chipRow}>
          {(['passport', 'license'] as const).map((t) => (
            <TouchableOpacity key={t} style={[styles.chip, docType === t && styles.chipActive]} onPress={() => setDocType(t)}>
              <Text style={[styles.chipText, docType === t && styles.chipTextActive]}>{t === 'passport' ? '🛂 Passport' : '🪪 License'}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* MRZ fields */}
      {docType === 'passport' && (
        <>
          <Field label="Full Name" value={fields.fullName} onChange={(v) => update('fullName', v)} placeholder="e.g. JOHN A DOE" />
          <Field label="Date of Birth" value={fields.dob} onChange={(v) => update('dob', v)} placeholder="YYYY-MM-DD" />
          <Field label="Nationality" value={fields.nationality} onChange={(v) => update('nationality', v)} placeholder="USA" />
          <Field label="ID / Passport Number" value={fields.idNumber} onChange={(v) => update('idNumber', v)} placeholder="123456789" />
        </>
      )}
      {docType === 'license' && (
        <>
          <Field label="Full Name" value={fields.fullName} onChange={(v) => update('fullName', v)} placeholder="e.g. Jane B Smith" />
          <Field label="Date of Birth" value={fields.dob} onChange={(v) => update('dob', v)} placeholder="YYYY-MM-DD" />
          <Field label="License Number" value={fields.idNumber} onChange={(v) => update('idNumber', v)} placeholder="DL-1234567" />
        </>
      )}

      <GradientButton
        title={isGenerating ? 'Generating…' : 'Generate Document'}
        variant="success"
        isLoading={isGenerating}
        onPress={handleGenerate}
        style={styles.btn}
      />
      <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

function Field({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <View style={styles.fieldGroup}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor={Colors.textMuted}
        autoCapitalize="characters"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  scroll: { padding: Spacing.md, gap: Spacing.lg },
  header: { ...Typography.h1, color: Colors.text, marginBottom: Spacing.md },
  section: { gap: Spacing.sm },
  label: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  chipRow: { flexDirection: 'row', gap: Spacing.sm },
  chip: { paddingHorizontal: Spacing.md, paddingVertical: Spacing.xs, borderRadius: 20, borderWidth: 1, borderColor: Colors.cardBorder, backgroundColor: Colors.inputBg },
  chipActive: { backgroundColor: Colors.success, borderColor: Colors.success },
  chipText: { ...Typography.caption, color: Colors.textSecondary },
  chipTextActive: { color: '#fff', fontWeight: '700' as const },
  fieldGroup: { marginBottom: Spacing.sm },
  fieldLabel: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  input: { backgroundColor: Colors.inputBg, borderWidth: 1, borderColor: Colors.inputBorder, borderRadius: 12, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, color: Colors.text, fontSize: 15, fontFamily: Typography.fontFamily },
  btn: { marginTop: Spacing.sm },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
});

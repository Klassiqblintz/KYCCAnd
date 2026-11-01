/**
 * Create Doc – Document generator with real MRZ (ICAO 9303) and ID card rendering.
 * Captures a face photo to embed in the ID card.
 */

import React, { useState, useRef } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, TextInput, Alert, Image,
} from 'react-native';
import { CameraView, CameraType, useCameraPermissions } from 'expo-camera';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';
import { computeMrz } from '../lib/mrz';
import { renderIdCard } from '../lib/idCard';

export default function CreateDocScreen() {
  const navigation = useNavigation<any>();
  const [docType, setDocType] = useState<'passport' | 'license'>('passport');
  const [isGenerating, setIsGenerating] = useState(false);
  const [fields, setFields] = useState({ fullName: '', dob: '', nationality: '', idNumber: '' });
  const [cardUri, setCardUri] = useState<string | null>(null);

  // Face capture state
  const [faceUri, setFaceUri] = useState<string | null>(null);
  const [showFaceCamera, setShowFaceCamera] = useState(false);
  const [captureFacing, setCaptureFacing] = useState<CameraType>('front');
  const cameraRef = useRef<any>(null);

  const [permission, requestPermission] = useCameraPermissions();

  const update = (key: string, val: string) => setFields(p => ({ ...p, [key]: val }));

  const captureFace = async () => {
    if (!cameraRef.current || !permission?.granted) return;
    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
      });
      if (photo) {
        setFaceUri(photo.uri);
        setShowFaceCamera(false);
      }
    } catch {
      Alert.alert('Error', 'Failed to capture face photo.');
    }
  };

  const handleGenerate = async () => {
    if (!fields.fullName || !fields.dob) {
      Alert.alert('Required', 'Name and DOB are required for MRZ generation.');
      return;
    }
    setIsGenerating(true);
    setCardUri(null);

    try {
      const mrzResult = computeMrz(docType, fields.fullName, fields.dob, fields.nationality, fields.idNumber, 'M');
      const cardPath = await renderIdCard({
        name: fields.fullName.toUpperCase(),
        dob: fields.dob,
        nationality: fields.nationality || 'USA',
        idNumber: fields.idNumber,
        mrzLines: mrzResult.lines,
        faceUri: faceUri || undefined,
      });
      setCardUri(cardPath);
      Alert.alert('Success', `ID card generated!\n${cardPath}`);
    } catch (err: any) {
      Alert.alert('Error', `Failed to generate document: ${err?.message || 'Unknown error'}`);
    } finally {
      setIsGenerating(false);
    }
  };

  // ── Render: Face capture mode ──────────────────────────────────
  if (showFaceCamera) {
    if (!permission) {
      return <View style={styles.centered}><Text style={styles.text}>Loading camera…</Text></View>;
    }

    if (!permission.granted) {
      return (
        <View style={styles.centered}>
          <Text style={styles.title}>📷 Camera Permission</Text>
          <Text style={styles.message}>
            Camera access needed to capture your face for the ID card.
          </Text>
          <GradientButton title="Grant Permission" onPress={requestPermission} style={{ marginTop: Spacing.lg }} />
        </View>
      );
    }

    return (
      <View style={styles.cameraOverlay}>
        <CameraView
          ref={(r: any) => { cameraRef.current = r; }}
          style={styles.fullCamera}
          facing={captureFacing}
        />
        <View style={styles.cameraControls}>
          <TouchableOpacity style={styles.cameraBtn} onPress={() => setShowFaceCamera(false)}>
            <Text style={styles.cameraBtnText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.captureBtn} onPress={captureFace}>
            <View style={styles.captureCircle} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.cameraBtn} onPress={() => setCaptureFacing(p => p === 'back' ? 'front' : 'back')}>
            <Text style={styles.cameraBtnText}>🔄</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ── Render: Main create doc screen ────────────────────────────
  if (!permission) {
    return <View style={styles.centered}><Text style={styles.text}>Loading…</Text></View>;
  }

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

      {/* Face photo capture */}
      <View style={styles.section}>
        <Text style={styles.label}>Face Photo (embedded in ID card)</Text>
        {faceUri ? (
          <View style={styles.facePreview}>
            <Image source={{ uri: faceUri }} style={styles.faceImage} resizeMode="cover" />
            <Text style={styles.faceLabel}>✅ Face photo captured</Text>
            <TouchableOpacity style={styles.retakeBtn} onPress={() => setFaceUri(null)}>
              <Text style={styles.retakeText}>✕ Retake</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <GradientButton
            title="📷 Capture Face Photo"
            variant="primary"
            onPress={() => setShowFaceCamera(true)}
          />
        )}
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

      {cardUri && (
        <View style={styles.cardPreview}>
          <Text style={styles.cardPreviewLabel}>📸 Generated ID Card</Text>
          <Image source={{ uri: cardUri }} style={styles.cardImage} resizeMode="contain" />
        </View>
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
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: Spacing.xl },
  title: { ...Typography.h2, color: Colors.text, marginBottom: Spacing.sm },
  message: { ...Typography.body, color: Colors.textSecondary, textAlign: 'center', marginBottom: Spacing.sm },
  text: { ...Typography.body, color: Colors.text },
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
  cardPreview: { alignItems: 'center', marginVertical: Spacing.md },
  cardPreviewLabel: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.sm },
  cardImage: { width: '100%', height: 540, borderRadius: 16, backgroundColor: Colors.card },
  facePreview: { alignItems: 'center', gap: Spacing.sm },
  faceImage: { width: '100%', height: 200, borderRadius: 12 },
  faceLabel: { ...Typography.caption, color: Colors.success },
  retakeBtn: { padding: Spacing.xs },
  retakeText: { ...Typography.caption, color: Colors.danger },
  // Camera overlay styles
  cameraOverlay: { flex: 1, backgroundColor: '#000' },
  fullCamera: { flex: 1 },
  cameraControls: {
    position: 'absolute', bottom: 40, left: 0, right: 0,
    flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center',
    paddingHorizontal: 40,
  },
  cameraBtn: { padding: Spacing.md },
  cameraBtnText: { fontSize: 24, color: '#fff' },
  captureBtn: { width: 72, height: 72, borderRadius: 36, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center' },
  captureCircle: { width: 60, height: 60, borderRadius: 30, borderWidth: 4, borderColor: '#000' },
});

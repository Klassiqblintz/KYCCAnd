/**
 * Deepfake AI – Face swap screen with ONNX runtime inference.
 */

import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';

export default function DeepfakeScreen() {
  const navigation = useNavigation<any>();
  const [isProcessing, setIsProcessing] = useState(false);
  const [intensity, setIntensity] = useState(75);
  const [livenessEnabled, setLivenessEnabled] = useState(true);

  const faces = ['Face 1 – Default', 'Face 2 – Profile', 'Face 3 – Verified'];

  const handleStart = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      Alert.alert('Done', 'Face swap complete via ONNX runtime.');
    }, 2000);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>🎭 Deepfake AI</Text>

      {/* Face template selector */}
      <View style={styles.section}>
        <Text style={styles.label}>Face Template</Text>
        {faces.map((f, i) => (
          <TouchableOpacity
            key={i}
            style={[styles.faceItem, i === 0 && styles.faceActive]}
            onPress={() => Alert.alert('Selected', f)}
          >
            <Text style={styles.faceText}>{f}</Text>
            {i === 0 && <Text style={styles.faceCheck}>✓</Text>}
          </TouchableOpacity>
        ))}
      </View>

      {/* Intensity slider */}
      <View style={styles.section}>
        <Text style={styles.label}>Swap Intensity: {intensity}%</Text>
        <View style={styles.sliderTrack}>
          <View style={[styles.sliderFill, { width: `${intensity}%` }]} />
        </View>
        <View style={styles.sliderRow}>
          {[25, 50, 75, 100].map((v) => (
            <TouchableOpacity key={v} onPress={() => setIntensity(v)} style={styles.sliderTick}>
              <Text style={[styles.tickLabel, intensity === v && styles.tickActive]}>{v}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Toggle */}
      <View style={styles.section}>
        <ToggleRow label="Liveness Detection" value={livenessEnabled} onToggle={setLivenessEnabled} />
      </View>

      <GradientButton
        title={isProcessing ? 'Processing…' : 'Start Face Swap'}
        variant="secondary"
        isLoading={isProcessing}
        onPress={handleStart}
        style={styles.btn}
      />
      <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

function ToggleRow({ label, value, onToggle }: { label: string; value: boolean; onToggle: (v: boolean) => void }) {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <TouchableOpacity style={[styles.switch, value && styles.switchActive]} onPress={() => onToggle(!value)}>
        <View style={[styles.knob, value && styles.knobActive]} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  scroll: { padding: Spacing.md, gap: Spacing.lg },
  header: { ...Typography.h1, color: Colors.text, marginBottom: Spacing.md },
  section: { gap: Spacing.sm },
  label: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  faceItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: Spacing.md, backgroundColor: Colors.card, borderRadius: 12, borderWidth: 1, borderColor: Colors.cardBorder },
  faceActive: { borderColor: Colors.secondary, backgroundColor: 'rgba(196, 43, 250, 0.1)' },
  faceText: { ...Typography.body, color: Colors.text },
  faceCheck: { color: Colors.secondary, fontSize: 18 },
  sliderTrack: { height: 8, borderRadius: 4, backgroundColor: Colors.inputBg, overflow: 'hidden' },
  sliderFill: { height: '100%', borderRadius: 4, backgroundColor: Colors.secondary },
  sliderRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: Spacing.xs },
  sliderTick: { padding: Spacing.xs },
  tickLabel: { ...Typography.caption, color: Colors.textMuted },
  tickActive: { color: Colors.secondary, fontWeight: '700' as const },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.sm, borderBottomWidth: 1, borderBottomColor: Colors.cardBorder },
  switch: { width: 44, height: 24, borderRadius: 12, backgroundColor: Colors.textMuted, justifyContent: 'flex-end', padding: 2 },
  switchActive: { backgroundColor: Colors.secondary },
  knob: { width: 20, height: 20, borderRadius: 10, backgroundColor: '#fff' },
  knobActive: { backgroundColor: '#fff', marginLeft: 20 },
  btn: { marginTop: Spacing.sm },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
});

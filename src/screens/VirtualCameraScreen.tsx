/**
 * Virtual Camera – captures and injects spoofed camera feeds via MediaProjection.
 */

import React, { useState, useRef } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert,
} from 'react-native';
import { Camera, CameraType, useCameraPermissions } from 'expo-camera';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';

export default function VirtualCameraScreen() {
  const navigation = useNavigation<any>();
  const [permission, requestPermission] = useCameraPermissions();
  const [facing, setFacing] = useState<CameraType>('back');
  const [isInjecting, setIsInjecting] = useState(false);
  const [loopMode, setLoopMode] = useState(false);
  const [mirrorMode, setMirrorMode] = useState(false);
  const [resolution, setResolution] = useState('1080p');

  const resolutions = ['720p', '1080p', '4K'] as const;

  if (!permission) {
    return <View style={styles.centered}><Text style={styles.text}>Loading…</Text></View>;
  }

  if (!permission.granted) {
    return (
      <View style={styles.centered}>
        <Text style={styles.title}>📷 Camera Permission</Text>
        <Text style={styles.message}>
          KYC Bypass needs camera access to capture and inject spoofed feeds.
        </Text>
        <GradientButton title="Grant Permission" onPress={requestPermission} style={{ marginTop: Spacing.lg }} />
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
          <Text style={styles.backText}>← Back to Menu</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const toggleFacing = () => setFacing((p) => (p === 'back' ? 'front' : 'back'));

  const startInject = () => {
    setIsInjecting(true);
    Alert.alert('Injecting', 'Camera feed is now being spoofed via MediaProjection.');
    setTimeout(() => setIsInjecting(false), 3000);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>📷 Virtual Camera</Text>

      {/* Camera preview */}
      <View style={styles.previewWrap}>
        {/* @ts-expect-error expo-camera Camera ref typing */}
        <Camera style={styles.camera} ref={(r: any) => r} facing={facing} mirror={mirrorMode} />
        <TouchableOpacity style={styles.flipBtn} onPress={toggleFacing}>
          <Text style={styles.flipIcon}>🔄</Text>
        </TouchableOpacity>
      </View>

      {/* Status bar */}
      <View style={styles.statusBar}>
        <View style={[styles.dot, isInjecting ? styles.dotActive : styles.dotInactive]} />
        <Text style={styles.statusText}>
          {isInjecting ? 'Injecting – 1080p' : 'Camera Ready'}
        </Text>
      </View>

      {/* Controls */}
      <View style={styles.section}>
        <Text style={styles.label}>Resolution</Text>
        <View style={styles.chipRow}>
          {resolutions.map((r) => (
            <TouchableOpacity
              key={r}
              style={[styles.chip, resolution === r && styles.chipActive]}
              onPress={() => setResolution(r)}
            >
              <Text style={[styles.chipText, resolution === r && styles.chipTextActive]}>{r}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <ToggleRow label="Loop Mode" value={loopMode} onToggle={setLoopMode} />
        <ToggleRow label="Mirror Mode" value={mirrorMode} onToggle={setMirrorMode} />
      </View>

      <GradientButton
        title={isInjecting ? 'Injecting…' : 'Start Inject'}
        variant="primary"
        isLoading={isInjecting}
        onPress={startInject}
        style={styles.btn}
      />

      <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

function ToggleRow({ label, value, onToggle }: {
  label: string; value: boolean; onToggle: (v: boolean) => void;
}) {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <TouchableOpacity
        style={[styles.switch, value && styles.switchActive]}
        onPress={() => onToggle(!value)}
      >
        <View style={[styles.knob, value && styles.knobActive]} />
      </TouchableOpacity>
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
  previewWrap: { position: 'relative', width: '100%', height: 240, borderRadius: 16, overflow: 'hidden', backgroundColor: Colors.card },
  camera: { flex: 1 },
  flipBtn: { position: 'absolute', bottom: Spacing.sm, right: Spacing.sm, width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  flipIcon: { fontSize: 20 },
  statusBar: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, padding: Spacing.sm, backgroundColor: Colors.card, borderRadius: 12 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  dotActive: { backgroundColor: Colors.success },
  dotInactive: { backgroundColor: Colors.textMuted },
  statusText: { ...Typography.caption, color: Colors.textSecondary },
  section: { gap: Spacing.sm },
  label: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  chipRow: { flexDirection: 'row', gap: Spacing.sm },
  chip: { paddingHorizontal: Spacing.md, paddingVertical: Spacing.xs, borderRadius: 20, borderWidth: 1, borderColor: Colors.cardBorder, backgroundColor: Colors.inputBg },
  chipActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  chipText: { ...Typography.caption, color: Colors.textSecondary },
  chipTextActive: { color: '#fff', fontWeight: '700' },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.sm, borderBottomWidth: 1, borderBottomColor: Colors.cardBorder },
  switch: { width: 44, height: 24, borderRadius: 12, backgroundColor: Colors.textMuted, justifyContent: 'flex-end', padding: 2 },
  switchActive: { backgroundColor: Colors.primary },
  knob: { width: 20, height: 20, borderRadius: 10, backgroundColor: '#fff' },
  knobActive: { backgroundColor: '#fff', marginLeft: 20 },
  btn: { marginTop: Spacing.sm },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
});

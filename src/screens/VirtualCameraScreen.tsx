/**
 * Virtual Camera – captures and injects spoofed camera feeds into Android's external camera.
 * Uses native Camera2 API to make our feed available system-wide.
 */

import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert,
  Image,
} from 'react-native';
import { CameraView, CameraType, useCameraPermissions } from 'expo-camera';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';
import {
  startInject,
  stopInject,
  injectFrameFromUri,
  isInjecting as checkInjecting,
  getFpsStats,
  FpsStats,
} from '../native/CameraInject';

// Resolution maps to camera pixel ratios
const resolutionConfig: Record<string, { label: string; width: number; height: number }> = {
  '720p': { label: '1280×720', width: 1280, height: 720 },
  '1080p': { label: '1920×1080', width: 1920, height: 1080 },
  '4K': { label: '3840×2160', width: 3840, height: 2160 },
};

export default function VirtualCameraScreen() {
  const navigation = useNavigation<any>();
  const [permission, requestPermission] = useCameraPermissions();
  const [facing, setFacing] = useState<CameraType>('back');
  const [isInjecting, setIsInjecting] = useState(false);
  const [loopMode, setLoopMode] = useState(false);
  const [mirrorMode, setMirrorMode] = useState(false);
  const [resolution, setResolution] = useState('1080p');
  const [frameCount, setFrameCount] = useState(0);
  const [capturedUri, setCapturedUri] = useState<string | null>(null);
  const [fpsStats, setFpsStats] = useState<FpsStats | null>(null);
  const [injectStatus, setInjectStatus] = useState<string>('');

  const cameraRef = useRef<any>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const injectRef = useRef(false);

  const resolutions = ['720p', '1080p', '4K'] as const;

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopInject();
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  // Update inject status when resolution changes
  useEffect(() => {
    if (isInjecting) {
      setInjectStatus(`Initializing ${resolutionConfig[resolution]?.label || resolution}...`);
    }
  }, [resolution, isInjecting]);

  if (!permission) {
    return <View style={styles.centered}><Text style={styles.text}>Loading camera…</Text></View>;
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

  const toggleFacing = () => setFacing(p => (p === 'back' ? 'front' : 'back'));

  const startInjectSession = async () => {
    const res = resolutionConfig[resolution] || resolutionConfig['1080p'];

    setInjectStatus('Starting system-wide injection...');
    setIsInjecting(true);
    setFrameCount(0);
    setCapturedUri(null);
    setFpsStats(null);

    try {
      const success = await startInject({
        width: res.width,
        height: res.height,
        fps: 15,
      });

      if (!success) {
        Alert.alert('Injection Error', 'Failed to start camera injection. Make sure you have an external camera available.');
        setIsInjecting(false);
        setInjectStatus('');
        return;
      }

      setInjectStatus(`Injecting at ${res.label}`);

      if (loopMode) {
        // Continuous loop: capture frames and inject continuously
        let frames = 0;
        const startTime = Date.now();
        intervalRef.current = setInterval(async () => {
          if (!injectRef.current) {
            clearInterval(intervalRef.current!);
            return;
          }

          if (!cameraRef.current) return;

          try {
            const photo = await cameraRef.current.takePictureAsync({
              quality: 0.6,
            });

            if (photo) {
              setCapturedUri(photo.uri);
              frames++;
              setFrameCount(frames);

              // Inject the frame into the external camera
              await injectFrameFromUri(photo.uri);

              // Update FPS stats periodically
              const elapsed = (Date.now() - startTime) / 1000;
              if (elapsed >= 1 && frames % 5 === 0) {
                const stats = await getFpsStats();
                if (stats) setFpsStats(stats);
              }
            }
          } catch {
            // Frame capture failed, skip
          }
        }, 66); // ~15 fps
      }
    } catch (err: any) {
      Alert.alert('Error', `Failed to start injection: ${err.message}`);
      setIsInjecting(false);
      setInjectStatus('');
    }
  };

  const stopInjectSession = async () => {
    injectRef.current = false;
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    try {
      await stopInject();
    } catch {
      // Ignore errors during stop
    }

    setIsInjecting(false);
    setInjectStatus('');
    setFpsStats(null);
  };

  const handleToggleInject = () => {
    if (isInjecting) {
      stopInjectSession();
    } else {
      injectRef.current = true;
      startInjectSession();
    }
  };

  const handleCaptureNow = async () => {
    if (!cameraRef.current) return;
    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
      });
      if (photo) {
        setCapturedUri(photo.uri);
        // If injecting, also inject this frame
        if (isInjecting) {
          await injectFrameFromUri(photo.uri);
          setFrameCount(prev => prev + 1);
        }
      }
    } catch (err: any) {
      Alert.alert('Error', `Capture failed: ${err.message}`);
    }
  };

  const resConfig = resolutionConfig[resolution] || resolutionConfig['1080p'];
  const actualFps = fpsStats?.actual ?? 0;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>📷 Virtual Camera</Text>

      {/* Camera preview */}
      <View style={styles.previewWrap}>
        <CameraView
          ref={(r: any) => { cameraRef.current = r; }}
          style={styles.camera}
          facing={facing}
          mirror={mirrorMode}
        />
        <TouchableOpacity style={styles.flipBtn} onPress={toggleFacing}>
          <Text style={styles.flipIcon}>🔄</Text>
        </TouchableOpacity>
      </View>

      {/* Captured frame preview */}
      {capturedUri && isInjecting && (
        <View style={styles.capturedPreview}>
          <Image source={{ uri: capturedUri }} style={styles.capturedImage} resizeMode="cover" />
          <Text style={styles.capturedLabel}>Latest Injected Frame</Text>
        </View>
      )}

      {/* Status bar */}
      <View style={styles.statusBar}>
        <View style={[styles.dot, isInjecting ? styles.dotActive : styles.dotInactive]} />
        <Text style={styles.statusText}>
          {injectStatus || (isInjecting ? 'Injecting...' : `Camera Ready – ${resConfig.label}`)}
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
              <Text style={[styles.chipText, resolution === r && styles.chipTextActive]}>
                {r} ({resolutionConfig[r]?.label})
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <ToggleRow label="Loop Mode (continuous injection)" value={loopMode} onToggle={setLoopMode} />
        <ToggleRow label="Mirror Mode" value={mirrorMode} onToggle={setMirrorMode} />
      </View>

      {/* Inject button */}
      <View style={styles.section}>
        {isInjecting ? (
          <GradientButton
            title="⏹ Stop Injection"
            variant="danger"
            onPress={stopInjectSession}
          />
        ) : (
          <GradientButton
            title="📡 Start System Injection"
            variant="primary"
            onPress={handleToggleInject}
          />
        )}

        {/* Manual capture button (when not injecting) */}
        {!isInjecting && (
          <GradientButton
            title="📸 Capture Frame"
            variant="secondary"
            onPress={handleCaptureNow}
            style={{ marginTop: Spacing.sm }}
          />
        )}
      </View>

      {/* Stats when injecting */}
      {isInjecting && (
        <View style={styles.statsBox}>
          <Text style={styles.statsTitle}>📊 System-Wide Injection</Text>
          <View style={styles.statusBadge}>
            <Text style={styles.statusBadgeText}>
              ✅ Active — other apps can now see your camera feed
            </Text>
          </View>
          <Text style={styles.statsRow}>
            Total Frames Injected: <Text style={styles.statsValue}>{frameCount}</Text>
          </Text>
          <Text style={styles.statsRow}>
            Actual Frame Rate: <Text style={styles.statsValue}>{actualFps} fps</Text>
          </Text>
          <Text style={styles.statsRow}>
            Resolution: <Text style={styles.statsValue}>{resConfig.label}</Text>
          </Text>
          <Text style={styles.statsRow}>
            Loop Mode: <Text style={styles.statsValue}>{loopMode ? 'On' : 'Off'}</Text>
          </Text>
          <Text style={styles.statsRow}>
            Mirror Mode: <Text style={styles.statsValue}>{mirrorMode ? 'On' : 'Off'}</Text>
          </Text>
        </View>
      )}

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
  previewWrap: { position: 'relative', width: '100%', height: 300, borderRadius: 16, overflow: 'hidden', backgroundColor: Colors.card },
  camera: { flex: 1 },
  flipBtn: { position: 'absolute', bottom: Spacing.sm, right: Spacing.sm, width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  flipIcon: { fontSize: 20 },
  capturedPreview: { alignItems: 'center', gap: Spacing.xs },
  capturedImage: { width: '100%', height: 200, borderRadius: 12, borderWidth: 2, borderColor: Colors.success },
  capturedLabel: { ...Typography.caption, color: Colors.success },
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
  statsBox: { padding: Spacing.md, backgroundColor: Colors.card, borderRadius: 12, borderWidth: 1, borderColor: Colors.cardBorder, gap: Spacing.sm },
  statsTitle: { ...Typography.h3, color: Colors.text, marginBottom: Spacing.xs },
  statsRow: { ...Typography.caption, color: Colors.textSecondary },
  statsValue: { color: Colors.primary, fontWeight: '600' },
  statusBadge: { padding: Spacing.sm, backgroundColor: 'rgba(0, 212, 155, 0.15)', borderRadius: 8 },
  statusBadgeText: { ...Typography.caption, color: Colors.success, fontWeight: '600' },
});

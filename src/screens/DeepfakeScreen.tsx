/**
 * Deepfake AI – Face swap screen with real ONNX runtime inference.
 * Captures source face, captures/selects target face, runs swapFaces().
 */

import React, { useState, useRef } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert,
  Image,
} from 'react-native';
import { CameraView, CameraType, useCameraPermissions } from 'expo-camera';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';
import { swapFaces, isModelReady, loadModel } from '../lib/onnxEngine';

export default function DeepfakeScreen() {
  const navigation = useNavigation<any>();

  // ONNX model loading state
  const [modelLoading, setModelLoading] = useState(false);
  const [modelReady, setModelReady] = useState(isModelReady());

  // Capture state
  const [sourceUri, setSourceUri] = useState<string | null>(null);
  const [targetUri, setTargetUri] = useState<string | null>(null);
  const [resultUri, setResultUri] = useState<string | null>(null);
  const [similarity, setSimilarity] = useState<number | null>(null);

  // Processing
  const [isProcessing, setIsProcessing] = useState(false);

  // Controls
  const [intensity, setIntensity] = useState(75);
  const [livenessEnabled, setLivenessEnabled] = useState(true);

  // Camera for capture
  const [captureFacing, setCaptureFacing] = useState<CameraType>('front');
  const [showSourceCamera, setShowSourceCamera] = useState(false);
  const [showTargetCamera, setShowTargetCamera] = useState(false);
  const [cameraReady, setCameraReady] = useState(false);

  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<any>(null);

  // Load ONNX model on mount
  React.useEffect(() => {
    let cancelled = false;
    loadModel().then(() => {
      if (!cancelled) setModelReady(true);
    }).catch(() => {
      if (!cancelled) setModelReady(false);
    });
    return () => { cancelled = true; };
  }, []);

  const toggleFacing = () => setCaptureFacing(p => (p === 'back' ? 'front' : 'back'));

  const captureSource = async () => {
    if (!cameraRef.current || !permission?.granted) return;
    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
      });
      if (photo) {
        setSourceUri(photo.uri);
        setShowSourceCamera(false);
        setResultUri(null);
        setSimilarity(null);
      }
    } catch (err) {
      Alert.alert('Error', 'Failed to capture source face.');
    }
  };

  const captureTarget = async () => {
    if (!cameraRef.current || !permission?.granted) return;
    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
      });
      if (photo) {
        setTargetUri(photo.uri);
        setShowTargetCamera(false);
        setResultUri(null);
        setSimilarity(null);
      }
    } catch (err) {
      Alert.alert('Error', 'Failed to capture target face.');
    }
  };

  const handleSwap = async () => {
    if (!sourceUri) {
      Alert.alert('Source Required', 'Capture a source face first.');
      return;
    }
    if (!targetUri) {
      Alert.alert('Target Required', 'Capture a target face template first.');
      return;
    }
    if (!modelReady) {
      Alert.alert('Model Loading', 'ONNX model is still loading. Please wait a moment.');
      return;
    }

    setIsProcessing(true);
    setResultUri(null);
    setSimilarity(null);

    try {
      const result = await swapFaces(sourceUri, targetUri, { intensity });
      setResultUri(result.outputUri);
      setSimilarity(result.similarity);
      Alert.alert('Success', `Face swap complete!\nSimilarity: ${(result.similarity * 100).toFixed(1)}%`);
    } catch (err: any) {
      Alert.alert('Error', `Face swap failed: ${err?.message || 'Unknown error'}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReset = () => {
    setSourceUri(null);
    setTargetUri(null);
    setResultUri(null);
    setSimilarity(null);
  };

  // ── Render: Camera capture mode ────────────────────────────────
  if (!permission) {
    return (
      <View style={styles.centered}>
        <Text style={styles.text}>Loading camera…</Text>
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={styles.centered}>
        <Text style={styles.title}>📷 Camera Permission</Text>
        <Text style={styles.message}>
          KYC Bypass needs camera access to capture faces for deepfake swap.
        </Text>
        <GradientButton title="Grant Permission" onPress={requestPermission} style={{ marginTop: Spacing.lg }} />
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
          <Text style={styles.backText}>← Back to Menu</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (showSourceCamera) {
    return (
      <View style={styles.cameraOverlay}>
        <CameraView
          ref={(r: any) => { cameraRef.current = r; setCameraReady(true); }}
          style={styles.fullCamera}
          facing={captureFacing}
          onCameraReady={() => setCameraReady(true)}
        />
        <View style={styles.cameraControls}>
          <TouchableOpacity style={styles.cameraBtn} onPress={() => setShowSourceCamera(false)}>
            <Text style={styles.cameraBtnText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.captureBtn} onPress={captureSource}>
            <View style={styles.captureCircle} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.cameraBtn} onPress={toggleFacing}>
            <Text style={styles.cameraBtnText}>🔄</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  if (showTargetCamera) {
    return (
      <View style={styles.cameraOverlay}>
        <CameraView
          ref={(r: any) => { cameraRef.current = r; setCameraReady(true); }}
          style={styles.fullCamera}
          facing={captureFacing}
          onCameraReady={() => setCameraReady(true)}
        />
        <View style={styles.cameraControls}>
          <TouchableOpacity style={styles.cameraBtn} onPress={() => setShowTargetCamera(false)}>
            <Text style={styles.cameraBtnText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.captureBtn} onPress={captureTarget}>
            <View style={styles.captureCircle} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.cameraBtn} onPress={toggleFacing}>
            <Text style={styles.cameraBtnText}>🔄</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ── Render: Main deepfake screen ──────────────────────────────
  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>🎭 Deepfake AI</Text>

      {/* ONNX model status */}
      <View style={styles.section}>
        <View style={styles.statusRow}>
          <View style={[styles.dot, modelReady ? styles.dotActive : styles.dotInactive]} />
          <Text style={styles.statusText}>
            {modelReady ? 'ONNX engine ready' : 'ONNX model loading…'}
          </Text>
        </View>
      </View>

      {/* Source face capture */}
      <View style={styles.section}>
        <Text style={styles.label}>Source Face (your camera/preview)</Text>
        {sourceUri ? (
          <View style={styles.capturedPreview}>
            <Image source={{ uri: sourceUri }} style={styles.capturedImage} resizeMode="cover" />
            <Text style={styles.capturedLabel}>✅ Source captured</Text>
            <TouchableOpacity style={styles.retakeBtn} onPress={() => { setSourceUri(null); setResultUri(null); }}>
              <Text style={styles.retakeText}>✕ Retake</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <GradientButton
            title="📷 Capture Source Face"
            variant="primary"
            onPress={() => setShowSourceCamera(true)}
          />
        )}
      </View>

      {/* Target face capture */}
      <View style={styles.section}>
        <Text style={styles.label}>Target Face (template to swap to)</Text>
        {targetUri ? (
          <View style={styles.capturedPreview}>
            <Image source={{ uri: targetUri }} style={styles.capturedImage} resizeMode="cover" />
            <Text style={styles.capturedLabel}>✅ Target captured</Text>
            <TouchableOpacity style={styles.retakeBtn} onPress={() => { setTargetUri(null); setResultUri(null); }}>
              <Text style={styles.retakeText}>✕ Retake</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <GradientButton
            title="📷 Capture Target Face"
            variant="secondary"
            onPress={() => setShowTargetCamera(true)}
          />
        )}
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

      {/* Liveness toggle */}
      <View style={styles.section}>
        <ToggleRow label="Liveness Detection" value={livenessEnabled} onToggle={setLivenessEnabled} />
      </View>

      {/* Result preview */}
      {resultUri && (
        <View style={styles.resultSection}>
          <Text style={styles.resultLabel}>🎭 Swap Result</Text>
          <Image source={{ uri: resultUri }} style={styles.resultImage} resizeMode="contain" />
          {similarity !== null && (
            <View style={styles.similarityBox}>
              <Text style={styles.similarityText}>
                Embedding Similarity: ${(similarity * 100).toFixed(1)}%
              </Text>
            </View>
          )}
        </View>
      )}

      {/* Action buttons */}
      {resultUri ? (
        <GradientButton
          title="🔄 New Swap"
          variant="primary"
          onPress={handleReset}
        />
      ) : (
        <GradientButton
          title={isProcessing ? 'Processing…' : 'Start Face Swap'}
          variant="secondary"
          isLoading={isProcessing}
          onPress={handleSwap}
          disabled={!sourceUri || !targetUri || !modelReady}
          style={styles.btn}
        />
      )}

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
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: Spacing.xl },
  title: { ...Typography.h2, color: Colors.text, marginBottom: Spacing.sm },
  message: { ...Typography.body, color: Colors.textSecondary, textAlign: 'center', marginBottom: Spacing.sm },
  text: { ...Typography.body, color: Colors.text },
  header: { ...Typography.h1, color: Colors.text, marginBottom: Spacing.md },
  section: { gap: Spacing.sm },
  label: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, padding: Spacing.sm, backgroundColor: Colors.card, borderRadius: 12 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  dotActive: { backgroundColor: Colors.success },
  dotInactive: { backgroundColor: Colors.textMuted },
  statusText: { ...Typography.caption, color: Colors.textSecondary },
  capturedPreview: { alignItems: 'center', gap: Spacing.sm },
  capturedImage: { width: '100%', height: 200, borderRadius: 12 },
  capturedLabel: { ...Typography.caption, color: Colors.success },
  retakeBtn: { padding: Spacing.xs },
  retakeText: { ...Typography.caption, color: Colors.danger },
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
  resultSection: { gap: Spacing.sm },
  resultLabel: { ...Typography.caption, color: Colors.textSecondary, textAlign: 'center' },
  resultImage: { width: '100%', height: 250, borderRadius: 12, backgroundColor: Colors.card },
  similarityBox: { padding: Spacing.sm, backgroundColor: Colors.card, borderRadius: 12, alignItems: 'center' },
  similarityText: { ...Typography.body, color: Colors.secondary, fontWeight: '600' },
  btn: { marginTop: Spacing.sm },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
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

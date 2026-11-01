/**
 * OBS Connect – RTSP/WebRTC stream viewer for spoofed camera feeds.
 * Shows live camera preview when disconnected, plays external stream when connected.
 */

import React, { useState, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, TextInput, Alert, TouchableOpacity, Dimensions } from 'react-native';
import { CameraView, CameraType, useCameraPermissions } from 'expo-camera';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';
import Video from 'react-native-video';

const { width } = Dimensions.get('window');

export default function OBSConnectScreen() {
  const navigation = useNavigation<any>();
  const videoRef = useRef<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [streamUrl, setStreamUrl] = useState('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isLivePreview, setIsLivePreview] = useState(false);
  const [facing, setFacing] = useState<CameraType>('front');
  const cameraRef = useRef<any>(null);

  const [permission, requestPermission] = useCameraPermissions();

  const buildStreamUrl = () => {
    if (streamUrl.startsWith('rtsp://') || streamUrl.startsWith('http://') || streamUrl.startsWith('https://')) {
      return streamUrl;
    }
    // If user just enters IP:port, prepend rtsp://
    if (streamUrl.includes(':')) return `rtsp://${streamUrl}`;
    // If just IP, add default port
    return `rtsp://${streamUrl}:554`;
  };

  const handleConnect = async () => {
    const url = buildStreamUrl();
    if (!url) {
      Alert.alert('Required', 'Enter a valid RTSP or HTTP stream URL.');
      return;
    }
    setStreamUrl(url);
    setIsConnected(true);
    setIsLivePreview(false);
    setIsLoading(true);
    setIsPlaying(false);
  };

  const handlePlay = () => {
    if (videoRef.current) {
      setIsPlaying(true);
    }
  };

  const handlePause = () => {
    if (videoRef.current) {
      setIsPlaying(false);
    }
  };

  const handleDisconnect = () => {
    setIsConnected(false);
    setIsPlaying(false);
    setIsLoading(false);
    setIsLivePreview(false);
    setStreamUrl('');
  };

  const handlePlaybackStateChanged = (data: { isPlaying: boolean; isSeeking: boolean }) => {
    setIsPlaying(data.isPlaying);
  };

  const toggleFacing = () => setFacing(p => p === 'back' ? 'front' : 'back');

  const isShowingLive = !isConnected && isLivePreview;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>📡 OBS Connect</Text>

      {/* Connection info */}
      <View style={styles.infoBox}>
        <Text style={styles.infoText}>
          Stream your spoofed camera feed to OBS Studio via RTSP or HTTP for virtual camera output.
        </Text>
        <Text style={styles.hint}>
          Enter the RTSP URL your camera app broadcasts. Example: rtsp://192.168.1.100:554/live
        </Text>
      </View>

      {/* Stream URL input */}
      <View style={styles.fieldGroup}>
        <Text style={styles.fieldLabel}>Stream URL (RTSP/HTTP)</Text>
        <TextInput
          style={styles.input}
          value={streamUrl}
          onChangeText={setStreamUrl}
          placeholder="rtsp://192.168.1.100:554/live"
          placeholderTextColor={Colors.textMuted}
          editable={!isConnected}
        />
      </View>

      {/* Player container */}
      <View style={styles.playerContainer}>
        <Text style={styles.playerLabel}>
          {isShowingLive ? '📷 Live Camera Preview' : 'Stream Preview'}
        </Text>
        <View style={styles.playerWrapper}>
          {isShowingLive ? (
            // Live camera preview
            <CameraView
              ref={(r: any) => { cameraRef.current = r; }}
              style={styles.video}
              facing={facing}
              mirror={facing === 'front'}
            />
          ) : streamUrl ? (
            // External stream
            <Video
              ref={videoRef}
              source={{ uri: streamUrl }}
              style={styles.video}
              resizeMode="contain"
              paused={!isPlaying}
              onBuffer={() => setIsLoading(true)}
              onEnd={handlePause}
              onError={(e: any) => {
                setIsPlaying(false);
                setIsLoading(false);
                const errorMsg = e?.nativeEvent?.error || e?.error || 'Unknown error';
                Alert.alert('Stream Error', `Failed to connect to stream.\n\n${JSON.stringify(errorMsg)}`);
              }}
              onReadyForDisplay={() => {
                setIsLoading(false);
                setIsPlaying(true);
              }}
              onLoad={() => setIsLoading(false)}
              onLoadStart={() => setIsLoading(true)}
              onPlaybackStateChanged={handlePlaybackStateChanged}
              muted={true}
            />
          ) : (
            // Placeholder
            <View style={styles.placeholder}>
              <Text style={styles.placeholderIcon}>📺</Text>
              <Text style={styles.placeholderText}>
                {isConnected ? 'Connecting...' : 'Enter a stream URL or enable live preview'}
              </Text>
            </View>
          )}
          {isLoading && !isShowingLive && (
            <View style={styles.loadingOverlay}>
              <Text style={styles.loadingText}>Loading stream...</Text>
            </View>
          )}
        </View>
      </View>

      {/* Camera controls for live preview */}
      {isShowingLive && (
        <View style={styles.cameraControlsRow}>
          <TouchableOpacity style={styles.cameraBtn} onPress={toggleFacing}>
            <Text style={styles.cameraBtnText}>🔄 Flip Camera</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.cameraBtn, { backgroundColor: Colors.danger }]} onPress={() => setIsLivePreview(false)}>
            <Text style={styles.cameraBtnText}>⏹ Stop Preview</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Status */}
      <View style={[styles.statusRow, isLivePreview ? styles.statusLive : (isConnected ? styles.statusActive : styles.statusInactive)]}>
        <View style={[styles.dot, isLivePreview ? styles.dotLive : (isConnected ? styles.dotActive : styles.dotInactive)]} />
        <Text style={[styles.statusText, isLivePreview ? styles.statusTextLive : (isConnected ? styles.statusTextActive : styles.statusTextInactive)]}>
          {isLivePreview ? 'Live camera preview active' : (isConnected ? (isPlaying ? 'Playing' : 'Connected (paused)') : 'Disconnected')}
        </Text>
      </View>

      {/* Buttons */}
      {isConnected ? (
        <View style={styles.actionRow}>
          {isPlaying ? (
            <GradientButton title="⏸ Pause" variant="warning" onPress={handlePause} style={styles.actionBtn} />
          ) : (
            <GradientButton title="▶ Play" variant="success" onPress={handlePlay} style={styles.actionBtn} />
          )}
          <GradientButton title="Disconnect" variant="danger" onPress={handleDisconnect} style={styles.actionBtn} />
        </View>
      ) : (
        <>
          <GradientButton title="Connect" variant="warning" onPress={handleConnect} style={styles.btn} />
          <GradientButton title="📷 Start Live Preview" variant="primary" onPress={() => {
            if (!permission?.granted) {
              requestPermission().then(() => setIsLivePreview(true));
            } else {
              setIsLivePreview(true);
            }
          }} style={styles.btn} />
        </>
      )}

      <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  scroll: { padding: Spacing.md, gap: Spacing.lg },
  header: { ...Typography.h1, color: Colors.text, marginBottom: Spacing.md },
  infoBox: { padding: Spacing.md, backgroundColor: Colors.card, borderRadius: 12, borderWidth: 1, borderColor: Colors.cardBorder },
  infoText: { ...Typography.body, color: Colors.textSecondary },
  hint: { ...Typography.caption, color: Colors.textMuted, marginTop: Spacing.xs },
  fieldGroup: { marginBottom: Spacing.sm },
  fieldLabel: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  input: { backgroundColor: Colors.inputBg, borderWidth: 1, borderColor: Colors.inputBorder, borderRadius: 12, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, color: Colors.text, fontSize: 15, fontFamily: Typography.fontFamily },
  playerContainer: { width: '100%' },
  playerLabel: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  playerWrapper: { width: width - 32, height: 220, backgroundColor: '#000', borderRadius: 12, overflow: 'hidden', justifyContent: 'center', alignItems: 'center', position: 'relative' },
  video: { width: '100%', height: '100%' },
  loadingOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.7)' },
  loadingText: { ...Typography.body, color: '#fff' },
  placeholder: { justifyContent: 'center', alignItems: 'center' },
  placeholderIcon: { fontSize: 48 },
  placeholderText: { ...Typography.body, color: Colors.textMuted, marginTop: Spacing.sm },
  cameraControlsRow: { flexDirection: 'row', gap: Spacing.sm },
  cameraBtn: { flex: 1, padding: Spacing.sm, borderRadius: 12, backgroundColor: Colors.card, borderWidth: 1, borderColor: Colors.cardBorder, alignItems: 'center' },
  cameraBtnText: { ...Typography.caption, color: Colors.text, fontWeight: '600' },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, padding: Spacing.sm, borderRadius: 12 },
  statusActive: { backgroundColor: 'rgba(0, 212, 155, 0.15)' },
  statusInactive: { backgroundColor: Colors.card },
  statusLive: { backgroundColor: 'rgba(8, 200, 237, 0.15)' },
  dot: { width: 10, height: 10, borderRadius: 5 },
  dotActive: { backgroundColor: Colors.success },
  dotInactive: { backgroundColor: Colors.textMuted },
  dotLive: { backgroundColor: Colors.primary },
  statusText: { ...Typography.caption },
  statusTextActive: { color: Colors.success },
  statusTextInactive: { color: Colors.textMuted },
  statusTextLive: { color: Colors.primary },
  btn: { marginTop: Spacing.sm },
  actionRow: { flexDirection: 'row', gap: Spacing.sm },
  actionBtn: { flex: 1 },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
});

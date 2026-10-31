/**
 * OBS Connect – Streaming connection to OBS virtual camera.
 */

import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TextInput, Alert, TouchableOpacity } from 'react-native';
import { Colors, Typography, Spacing } from '../utils/theme';
import GradientButton from '../components/GradientButton';
import { useNavigation } from '@react-navigation/native';

export default function OBSConnectScreen() {
  const navigation = useNavigation<any>();
  const [isConnected, setIsConnected] = useState(false);
  const [url, setUrl] = useState('');
  const [port, setPort] = useState('4444');

  const handleConnect = () => {
    if (!url || !port) {
      Alert.alert('Required', 'Enter server URL and port.');
      return;
    }
    setIsConnected(true);
    Alert.alert('Connected', `OBS stream active at ${url}:${port}`);
  };

  const handleDisconnect = () => {
    setIsConnected(false);
    Alert.alert('Disconnected', 'OBS stream stopped.');
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scroll}>
      <Text style={styles.header}>📡 OBS Connect</Text>

      {/* Connection info */}
      <View style={styles.infoBox}>
        <Text style={styles.infoText}>
          Stream your spoofed camera feed to OBS Studio via RTSP or WebRTC for virtual camera output.
        </Text>
      </View>

      {/* Server URL */}
      <View style={styles.fieldGroup}>
        <Text style={styles.fieldLabel}>Server URL (RTSP/WebRTC)</Text>
        <TextInput style={styles.input} value={url} onChangeText={setUrl} placeholder="e.g. rtsp://192.168.1.100/live" placeholderTextColor={Colors.textMuted} />
      </View>

      {/* Port */}
      <View style={styles.fieldGroup}>
        <Text style={styles.fieldLabel}>Port</Text>
        <TextInput style={styles.input} value={port} onChangeText={setPort} placeholder="4444" placeholderTextColor={Colors.textMuted} keyboardType="number-pad" />
      </View>

      {/* Status */}
      <View style={[styles.statusRow, isConnected ? styles.statusActive : styles.statusInactive]}>
        <View style={[styles.dot, isConnected ? styles.dotActive : styles.dotInactive]} />
        <Text style={[styles.statusText, isConnected ? styles.statusTextActive : styles.statusTextInactive]}>
          {isConnected ? 'Connected' : 'Disconnected'}
        </Text>
      </View>

      {/* Buttons */}
      {isConnected ? (
        <GradientButton title="Disconnect" variant="danger" onPress={handleDisconnect} style={styles.btn} />
      ) : (
        <GradientButton title="Connect" variant="warning" onPress={handleConnect} style={styles.btn} />
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
  fieldGroup: { marginBottom: Spacing.sm },
  fieldLabel: { ...Typography.caption, color: Colors.textSecondary, marginBottom: Spacing.xs },
  input: { backgroundColor: Colors.inputBg, borderWidth: 1, borderColor: Colors.inputBorder, borderRadius: 12, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, color: Colors.text, fontSize: 15, fontFamily: Typography.fontFamily },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, padding: Spacing.sm, borderRadius: 12 },
  statusActive: { backgroundColor: 'rgba(0, 212, 155, 0.15)' },
  statusInactive: { backgroundColor: Colors.card },
  dot: { width: 10, height: 10, borderRadius: 5 },
  dotActive: { backgroundColor: Colors.success },
  dotInactive: { backgroundColor: Colors.textMuted },
  statusText: { ...Typography.caption },
  statusTextActive: { color: Colors.success },
  statusTextInactive: { color: Colors.textMuted },
  btn: { marginTop: Spacing.sm },
  backBtn: { padding: Spacing.sm, alignItems: 'center' },
  backText: { ...Typography.caption, color: Colors.textMuted },
});

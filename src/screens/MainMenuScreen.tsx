/**
 * Main Menu – the home screen with all 5 feature cards.
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Platform,
  StatusBar,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { Colors, Typography, Spacing } from '../utils/theme';
import FeatureCard from '../components/FeatureCard';

export default function MainMenuScreen() {
  const navigation = useNavigation<any>();

  const features = [
    {
      id: 'camera',
      title: 'Virtual Camera (Inject)',
      description: 'Inject spoofed camera feeds into any app',
      icon: '\uD83D\uDCF7',
      iconColor: Colors.primary,
      screen: 'VirtualCamera',
    },
    {
      id: 'deepfake',
      title: 'Deepfake AI (Face Swap)',
      description: 'Real-time face swap using ONNX engine',
      icon: '\uD83C\uDFAD',
      iconColor: Colors.secondary,
      screen: 'Deepfake',
    },
    {
      id: 'docgen',
      title: 'Create Doc (Doc Generator)',
      description: "Generate passports, driver's licenses, MRZ",
      icon: '\uD83E\uDD6A',
      iconColor: Colors.success,
      screen: 'CreateDoc',
    },
    {
      id: 'obs',
      title: 'OBS Connect',
      description: 'Stream spoofed feed to OBS virtual cam',
      icon: '\uD83D\uDCE1',
      iconColor: Colors.warning,
      screen: 'OBSConnect',
    },
  ];

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.logoIcon}>🛡️</Text>
        <Text style={styles.appTitle}>KYC Bypass Suite</Text>
        <Text style={styles.appVersion}>v1.0.0 • Android</Text>
      </View>

      {/* Feature cards */}
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {features.map((f) => (
          <FeatureCard
            key={f.id}
            title={f.title}
            description={f.description}
            icon={f.icon}
            iconColor={f.iconColor}
            onPress={() => navigation.navigate(f.screen as any)}
            style={styles.card}
          />
        ))}

        {/* Footer info */}
        <View style={styles.footer}>
          <Text style={styles.footerText}>On-device engine • No cloud • Root bypass</Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    alignItems: 'center',
    paddingTop: 60,
    paddingBottom: 24,
  },
  logoIcon: {
    fontSize: 48,
    marginBottom: 8,
  },
  appTitle: {
    ...Typography.h1,
    color: Colors.text,
    textAlign: 'center',
  },
  appVersion: {
    ...Typography.caption,
    color: Colors.textMuted,
    marginTop: 4,
  },
  scrollContent: {
    padding: Spacing.md,
    paddingBottom: 40,
  },
  card: {
    marginBottom: Spacing.sm,
  },
  footer: {
    marginTop: Spacing.xl,
    alignItems: 'center',
  },
  footerText: {
    ...Typography.caption,
    color: Colors.textMuted,
  },
});

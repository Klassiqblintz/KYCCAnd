/**
 * Feature card – the main navigation tile on the home screen.
 */

import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ViewStyle,
  TextStyle,
  TouchableOpacityProps,
} from 'react-native';
import { Colors, Typography, Spacing } from '../utils/theme';

interface FeatureCardProps extends Omit<TouchableOpacityProps, 'style'> {
  title: string;
  description: string;
  icon: string;
  iconColor: string;
  onPress: () => void;
  style?: ViewStyle;
}

export default function FeatureCard({
  title,
  description,
  icon,
  iconColor,
  onPress,
  style,
}: FeatureCardProps) {
  return (
    <TouchableOpacity style={[styles.card, style]} onPress={onPress} activeOpacity={0.7}>
      <View style={[styles.iconCircle, { backgroundColor: iconColor + '20' }]}>
        <Text style={styles.icon}>{icon}</Text>
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.description}>{description}</Text>
      </View>
      <Text style={styles.arrow}>›</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.card,
    borderRadius: 16,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    gap: Spacing.md,
  },
  iconCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    flexShrink: 0,
  },
  icon: {
    fontSize: 22,
  },
  content: {
    flex: 1,
    gap: 2,
  },
  title: {
    ...Typography.h3,
    color: Colors.text,
  },
  description: {
    ...Typography.caption,
    color: Colors.textSecondary,
  },
  arrow: {
    fontSize: 24,
    color: Colors.textMuted,
  },
});

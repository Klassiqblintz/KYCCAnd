/**
 * Reusable gradient-style button component for the dark theme.
 */

import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
  ViewStyle,
  TextStyle,
  TouchableOpacityProps,
} from 'react-native';
import { Colors, Typography } from '../utils/theme';

interface GradientButtonProps extends Omit<TouchableOpacityProps, 'style'> {
  title: string;
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger';
  icon?: string;
  isLoading?: boolean;
  style?: ViewStyle;
  textStyle?: TextStyle;
}

const variantColors: Record<string, { bg: string; border: string }> = {
  primary: { bg: Colors.primary, border: Colors.primaryDark },
  secondary: { bg: Colors.secondary, border: Colors.secondaryDark },
  success: { bg: Colors.success, border: '#00b886' },
  warning: { bg: Colors.warning, border: '#d49400' },
  danger: { bg: Colors.danger, border: '#d4355f' },
};

export default function GradientButton({
  title,
  variant = 'primary',
  icon,
  isLoading = false,
  style,
  textStyle,
  disabled,
  ...props
}: GradientButtonProps) {
  const colors = variantColors[variant] || variantColors.primary;

  return (
    <TouchableOpacity
      style={[
        styles.button,
        {
          backgroundColor: disabled ? '#2a2545' : colors.bg,
          borderColor: disabled ? '#2a2545' : colors.border,
        },
        style,
      ]}
      disabled={disabled || isLoading}
      {...props}
    >
      {icon && <Text style={styles.icon}>{icon}</Text>}
      {isLoading ? (
        <ActivityIndicator color="#fff" />
      ) : (
        <Text style={[styles.text, textStyle]}>{title}</Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderRadius: 12,
    borderWidth: 2,
    minHeight: 50,
    gap: 8,
  },
  icon: {
    fontSize: 18,
  },
  text: {
    ...Typography.body,
    color: '#fff',
    fontWeight: '600',
  },
});

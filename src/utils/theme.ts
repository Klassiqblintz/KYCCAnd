/**
 * KYC Bypass Suite – Design tokens (dark theme)
 * All colors from the specification.
 */

export const Colors = {
  background: '#090617',
  card: '#13102a',
  cardBorder: '#1e1a3a',
  primary: '#08c8ed',
  primaryDark: '#06a0c0',
  secondary: '#c42bfa',
  secondaryDark: '#9a22c6',
  success: '#00d49b',
  warning: '#ffad00',
  danger: '#f43f72',
  text: '#ffffff',
  textSecondary: '#a09cb8',
  textMuted: '#5e5a78',
  inputBg: '#0d0b1a',
  inputBorder: '#1e1a3a',
  overlay: 'rgba(9, 6, 23, 0.85)',
} as const;

export const Typography = {
  fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  h1: { fontSize: 28, fontWeight: '800' as const, lineHeight: 36 },
  h2: { fontSize: 22, fontWeight: '700' as const, lineHeight: 30 },
  h3: { fontSize: 18, fontWeight: '600' as const, lineHeight: 26 },
  body: { fontSize: 15, fontWeight: '400' as const, lineHeight: 22 },
  caption: { fontSize: 12, fontWeight: '400' as const, lineHeight: 18 },
} as const;

export const Spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

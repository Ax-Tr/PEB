// PEB Application Theme - Premium Design System

export const COLORS = {
  // Primary gradient palette
  primary: '#6C63FF',
  primaryDark: '#5A52D5',
  primaryLight: '#8B85FF',
  primaryGlow: 'rgba(108, 99, 255, 0.15)',

  // Accent colors
  accent: '#00D9A6',
  accentDark: '#00B88A',
  accentLight: '#33E4BC',

  // Semantic colors
  success: '#00D9A6',
  successLight: 'rgba(0, 217, 166, 0.12)',
  warning: '#FFB547',
  warningLight: 'rgba(255, 181, 71, 0.12)',
  error: '#FF6B6B',
  errorLight: 'rgba(255, 107, 107, 0.12)',
  info: '#4DA6FF',
  infoLight: 'rgba(77, 166, 255, 0.12)',

  // Background hierarchy
  background: '#0A0A1A',
  surface: '#12122A',
  surfaceElevated: '#1A1A3E',
  surfaceHighlight: '#222252',
  card: '#16163A',
  cardBorder: 'rgba(108, 99, 255, 0.2)',

  // Text hierarchy
  textPrimary: '#FFFFFF',
  textSecondary: '#A0A0C0',
  textTertiary: '#6B6B8D',
  textInverse: '#0A0A1A',

  // Gradients (as arrays for LinearGradient)
  gradientPrimary: ['#6C63FF', '#A855F7'],
  gradientAccent: ['#00D9A6', '#00B4D8'],
  gradientWarm: ['#FF6B6B', '#FFB547'],
  gradientDark: ['#0A0A1A', '#12122A'],
  gradientCard: ['rgba(108, 99, 255, 0.08)', 'rgba(168, 85, 247, 0.04)'],

  // Utility
  divider: 'rgba(255, 255, 255, 0.06)',
  overlay: 'rgba(0, 0, 0, 0.5)',
  shimmer: 'rgba(255, 255, 255, 0.05)',
};

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32,
  huge: 40,
  massive: 48,
};

export const FONT_SIZES = {
  caption: 11,
  small: 12,
  body: 14,
  medium: 16,
  large: 18,
  title: 22,
  heading: 26,
  hero: 32,
  display: 40,
};

export const BORDER_RADIUS = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  round: 50,
  full: 999,
};

export const SHADOWS = {
  sm: {
    shadowColor: '#6C63FF',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  md: {
    shadowColor: '#6C63FF',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 8,
    elevation: 4,
  },
  lg: {
    shadowColor: '#6C63FF',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.16,
    shadowRadius: 16,
    elevation: 8,
  },
  glow: {
    shadowColor: '#6C63FF',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.3,
    shadowRadius: 20,
    elevation: 10,
  },
};

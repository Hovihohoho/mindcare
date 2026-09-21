/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * genre: modern-minimal · macrostructure: Native Content Flow · design-system: design.md · designed-as-app
 * mobile: 320–768 dp · states: loading · empty · error · ready · selected
 */
import { Platform } from 'react-native';

export const colors = {
  transparent: 'transparent',
  canvas: '#F7FBF8',
  homeCanvas: '#F7FBF8',
  surface: '#FFFEFC',
  surfaceMuted: '#EEF4EF',
  surfacePressed: '#E2F2EA',
  ink: '#263833',
  inkSoft: '#465850',
  muted: '#66756E',
  tertiary: '#94A39B',
  line: '#D9E7DA',
  lineStrong: '#BDD6C2',
  brand: '#3D7C63',
  brandDark: '#287060',
  brandDeep: '#245D50',
  brandSoft: '#E2F2EA',
  accentInk: '#FFFFFF',
  mint: '#DDF2E4',
  mintInk: '#3D7C63',
  warningSoft: '#FFF0D9',
  warning: '#8A5B2B',
  dangerSoft: '#F9EEEE',
  dangerLine: '#EBCACA',
  danger: '#A14B4A',
  washBlue: '#DDF2F7',
  washMint: '#E2F6ED',
  washBlush: '#F8E9ED',
  washSand: '#F8F0E4',
  washLavender: '#ECE9F7',
  emotionVeryHappy: '#22C55E',
  emotionVeryHappySoft: '#F0FDF4',
  emotionHappy: '#84CC16',
  emotionHappySoft: '#F7FEE7',
  emotionNeutral: '#5B8DEF',
  emotionNeutralSoft: '#EEF3FE',
  emotionSad: '#D6A84B',
  emotionSadSoft: '#FCF6E9',
  emotionStressed: '#D96868',
  emotionStressedSoft: '#FCEEEE',
  emotionFaceInk: '#FFFFFF',
  skeleton: '#E5EEE7',
  overlay: 'rgba(38, 56, 51, 0.38)',
} as const;

export const fonts = {
  regular: 'BeVietnamPro_400Regular',
  medium: 'BeVietnamPro_500Medium',
  semibold: 'BeVietnamPro_600SemiBold',
  bold: 'BeVietnamPro_700Bold',
} as const;

export const spacing = {
  xxs: 4,
  xs: 8,
  sm: 12,
  md: 16,
  page: 20,
  lg: 24,
  xl: 32,
  xxl: 40,
} as const;

export const radius = {
  sm: 8,
  input: 12,
  card: 12,
  sheet: 16,
  pill: 999,
} as const;

export const type = {
  title: 30,
  homeTitle: 26,
  homeTitleLine: 34,
  section: 20,
  cardTitle: 16,
  body: 15,
  label: 13,
  caption: 12,
  tab: 11,
} as const;

export const shadows = {
  card: Platform.select({ ios: {}, android: {}, default: {} }),
  sheet: Platform.select({
    ios: { shadowColor: '#173446', shadowOpacity: 0.16, shadowRadius: 24, shadowOffset: { width: 0, height: -8 } },
    android: { elevation: 16 },
    default: { boxShadow: '0 -8px 28px rgba(23, 52, 70, 0.16)' },
  }),
} as const;

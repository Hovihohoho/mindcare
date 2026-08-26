/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * genre: modern-minimal · macrostructure: Native Content Flow · design-system: design.md · designed-as-app
 * mobile: 320–768 dp · states: loading · empty · error · ready · selected
 */
import { Platform } from 'react-native';

export const colors = {
  transparent: 'transparent',
  canvas: '#F7FAF9',
  surface: '#FFFFFF',
  surfaceMuted: '#E7F2F0',
  surfacePressed: '#DCECE9',
  ink: '#172B2D',
  inkSoft: '#435A5C',
  muted: '#667779',
  tertiary: '#8C999A',
  line: '#E3E9E8',
  lineStrong: '#C7D3D1',
  brand: '#397C78',
  brandDark: '#245C59',
  brandDeep: '#1C4947',
  brandSoft: '#E7F2F0',
  accentInk: '#FFFFFF',
  mint: '#E8F4ED',
  mintInk: '#39795B',
  warningSoft: '#FFF4D6',
  warning: '#946200',
  dangerSoft: '#FDECEF',
  dangerLine: '#F5BEC8',
  danger: '#B4233D',
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
  skeleton: '#E6ECEB',
  overlay: 'rgba(23, 43, 45, 0.38)',
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
  sm: 10,
  input: 12,
  card: 14,
  sheet: 22,
  pill: 999,
} as const;

export const type = {
  title: 30,
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

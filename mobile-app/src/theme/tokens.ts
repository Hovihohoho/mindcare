/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V5
 * genre: modern-minimal · macrostructure: Index-First · tone: soft clinical · anchor hue: MindCare blue
 * mobile: 360–430 dp · states: loading · empty · error · ready · filtering
 */
import { Platform } from 'react-native';

export const colors = {
  transparent: 'transparent',
  canvas: '#F4FBFD',
  surface: '#FAFDFE',
  surfaceMuted: '#D4F4FF',
  surfacePressed: '#C4EAF6',
  ink: '#173446',
  inkSoft: '#35586B',
  muted: '#526F7E',
  line: '#BEDCE6',
  lineStrong: '#6A96A9',
  brand: '#5FA8D3',
  brandDark: '#286C93',
  brandDeep: '#174B69',
  brandSoft: '#D4F4FF',
  mint: '#A3F0CB',
  mintInk: '#195D43',
  warningSoft: '#FFF4D6',
  warning: '#946200',
  dangerSoft: '#FDECEF',
  dangerLine: '#F5BEC8',
  danger: '#B4233D',
  emotionVeryHappy: '#22C55E',
  emotionVeryHappySoft: '#F0FDF4',
  emotionHappy: '#84CC16',
  emotionHappySoft: '#F7FEE7',
  emotionNeutral: '#3B82F6',
  emotionNeutralSoft: '#EFF6FF',
  emotionSad: '#FACC15',
  emotionSadSoft: '#FEFCE8',
  emotionStressed: '#F43F5E',
  emotionStressedSoft: '#FFF1F2',
  emotionFaceInk: '#FAFDFE',
  skeleton: '#DDECF1',
  overlay: 'rgba(23, 52, 70, 0.38)',
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
  lg: 24,
  xl: 32,
  xxl: 40,
} as const;

export const radius = {
  sm: 10,
  input: 12,
  card: 16,
  sheet: 24,
  pill: 999,
} as const;

export const type = {
  title: 28,
  section: 20,
  cardTitle: 16,
  body: 15,
  label: 13,
  caption: 12,
  tab: 10,
} as const;

export const shadows = {
  card: Platform.select({
    ios: { shadowColor: '#173446', shadowOpacity: 0.06, shadowRadius: 10, shadowOffset: { width: 0, height: 3 } },
    android: { elevation: 2 },
    default: { boxShadow: '0 3px 12px rgba(23, 52, 70, 0.06)' },
  }),
  sheet: Platform.select({
    ios: { shadowColor: '#173446', shadowOpacity: 0.16, shadowRadius: 24, shadowOffset: { width: 0, height: -8 } },
    android: { elevation: 16 },
    default: { boxShadow: '0 -8px 28px rgba(23, 52, 70, 0.16)' },
  }),
} as const;

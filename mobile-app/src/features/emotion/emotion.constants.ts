import { colors } from '@/theme/tokens';
import type { EmotionLevel } from './emotion.types';

export type EmotionFace = 'laugh' | 'smile' | 'neutral' | 'sad' | 'stressed';

export type EmotionOption = {
  value: EmotionLevel;
  label: string;
  compactLabel: string;
  face: EmotionFace;
  color: string;
  surface: string;
  score: number;
};

export const emotionOptions: EmotionOption[] = [
  { value: 'VERY_HAPPY', label: 'Rất vui', compactLabel: 'Rất vui', face: 'laugh', color: colors.emotionVeryHappy, surface: colors.emotionVeryHappySoft, score: 2 },
  { value: 'HAPPY', label: 'Vui', compactLabel: 'Vui', face: 'smile', color: colors.emotionHappy, surface: colors.emotionHappySoft, score: 1 },
  { value: 'NEUTRAL', label: 'Bình thường', compactLabel: 'Ổn', face: 'neutral', color: colors.emotionNeutral, surface: colors.emotionNeutralSoft, score: 0 },
  { value: 'SAD', label: 'Buồn', compactLabel: 'Buồn', face: 'sad', color: colors.emotionSad, surface: colors.emotionSadSoft, score: -1 },
  { value: 'STRESSED', label: 'Căng thẳng', compactLabel: 'Căng', face: 'stressed', color: colors.emotionStressed, surface: colors.emotionStressedSoft, score: -2 },
];

export const emotionOptionMap = Object.fromEntries(emotionOptions.map((option) => [option.value, option])) as Record<EmotionLevel, EmotionOption>;

export function optionFromScore(score: number | null) {
  if (score === null || !Number.isFinite(score)) return undefined;
  if (score >= 2) return emotionOptionMap.VERY_HAPPY;
  if (score >= 1) return emotionOptionMap.HAPPY;
  if (score >= 0) return emotionOptionMap.NEUTRAL;
  if (score >= -1) return emotionOptionMap.SAD;
  return emotionOptionMap.STRESSED;
}

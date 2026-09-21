import { Pressable, StyleSheet, Text, View } from 'react-native';
import { palette, radius, space, type } from '../theme/tokens';

type EmotionFace = 'laugh' | 'smile' | 'neutral' | 'sad' | 'stressed';
export type EmotionLevel = 'VERY_HAPPY' | 'HAPPY' | 'NEUTRAL' | 'SAD' | 'STRESSED';

const options: Array<{ value: EmotionLevel; label: string; face: EmotionFace; color: string; score: number }> = [
  { value: 'VERY_HAPPY', label: 'Rất vui', face: 'laugh', color: palette.emotionVeryHappy, score: 2 },
  { value: 'HAPPY', label: 'Vui', face: 'smile', color: palette.emotionHappy, score: 1 },
  { value: 'NEUTRAL', label: 'Ổn', face: 'neutral', color: palette.emotionNeutral, score: 0 },
  { value: 'SAD', label: 'Buồn', face: 'sad', color: palette.emotionSad, score: -1 },
  { value: 'STRESSED', label: 'Căng', face: 'stressed', color: palette.emotionStressed, score: -2 },
];

const levels = [
  { score: 2, label: 'Rất vui' },
  { score: 1, label: 'Vui' },
  { score: 0, label: 'Bình thường' },
  { score: -1, label: 'Buồn' },
  { score: -2, label: 'Căng thẳng' },
] as const;

const PLOT_HEIGHT = 180;

export function EmotionPicker({ value, onChange }: { value: EmotionLevel; onChange(value: EmotionLevel): void }) {
  return <View accessibilityLabel="Chọn cảm xúc hôm nay" accessibilityRole="radiogroup" style={styles.picker}>
    {options.map((option) => {
      const selected = option.value === value;
      return <Pressable accessibilityLabel={option.label} accessibilityRole="radio" accessibilityState={{ checked: selected }} key={option.value} onPress={() => onChange(option.value)} style={({ pressed }) => [styles.option, selected && styles.optionSelected, pressed && styles.optionPressed]}>
        <EmotionFaceIcon color={option.color} face={option.face} />
        <Text numberOfLines={1} style={[styles.optionLabel, selected && styles.optionLabelSelected]}>{option.label}</Text>
      </Pressable>;
    })}
  </View>;
}

export function EmotionTrendChart({ points }: { points: Array<{ day: string; score: number | null; count: number }> }) {
  return <View accessibilityLabel="Biểu đồ cảm xúc 7 ngày" style={styles.chartRow}>
    <View style={styles.axis}>{levels.map((level, index) => <View key={level.score} style={[styles.axisLevel, { top: index * (PLOT_HEIGHT / (levels.length - 1)) }, index > 0 && index < levels.length - 1 && styles.axisMiddle, index === levels.length - 1 && styles.axisLast]}><Text numberOfLines={1} style={styles.axisLabel}>{level.label}</Text></View>)}</View>
    <View style={styles.plotWrap}>
      <View style={styles.plotArea}>
        <View pointerEvents="none" style={styles.grid}>{levels.map((level) => <View key={level.score} style={styles.gridLine} />)}</View>
        <View style={styles.bars}>{points.map((point) => {
          const score = point.score === null ? -2 : Math.max(-2, Math.min(2, point.score));
          const option = options.find((item) => item.score === score) ?? options[4];
          return <View accessibilityLabel={point.count ? `${point.day}, ${option.label}, ${point.count} bản ghi` : `${point.day}, chưa có dữ liệu`} key={point.day} style={styles.barColumn}><View style={[styles.bar, { backgroundColor: point.count ? option.color : palette.line, height: `${Math.max(3, ((score + 2) / 4) * 100)}%` }]} /></View>;
        })}</View>
      </View>
      <View style={styles.dayRow}>{points.map((point) => <Text key={point.day} style={styles.day}>{point.day}</Text>)}</View>
    </View>
  </View>;
}

function EmotionFaceIcon({ face, color, size = 36 }: { face: EmotionFace; color: string; size?: number }) {
  const eyeTop = size * 0.31;
  const eyeSize = Math.max(3, size * 0.1);
  const mouthWidth = size * 0.42;
  return <View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" style={[styles.face, { backgroundColor: color, borderRadius: size / 2, height: size, width: size }]}>
    {face === 'stressed' ? <><CrossEye left={size * 0.25} size={eyeSize + 2} top={eyeTop} /><CrossEye left={size * 0.62} size={eyeSize + 2} top={eyeTop} /></> : face === 'laugh' ? <><LaughEye left={size * 0.22} size={eyeSize + 4} top={eyeTop} /><LaughEye left={size * 0.61} size={eyeSize + 4} top={eyeTop} /></> : <><View style={[styles.eye, { borderRadius: eyeSize / 2, height: eyeSize, left: size * 0.27, top: eyeTop, width: eyeSize }]} /><View style={[styles.eye, { borderRadius: eyeSize / 2, height: eyeSize, right: size * 0.27, top: eyeTop, width: eyeSize }]} /></>}
    <View style={[styles.mouth, { left: (size - mouthWidth) / 2, width: mouthWidth }, face === 'laugh' && { backgroundColor: palette.emotionFaceInk, borderRadius: size, height: size * 0.22, top: size * 0.54 }, face === 'smile' && { borderBottomColor: palette.emotionFaceInk, borderBottomWidth: 2, borderRadius: size, height: size * 0.2, top: size * 0.49 }, face === 'neutral' && { backgroundColor: palette.emotionFaceInk, borderRadius: 2, height: 2, top: size * 0.62 }, (face === 'sad' || face === 'stressed') && { borderRadius: size, borderTopColor: palette.emotionFaceInk, borderTopWidth: 2, height: size * 0.18, top: size * 0.59 }]} />
  </View>;
}

function LaughEye({ left, size, top }: { left: number; size: number; top: number }) { return <View style={{ height: size, left, position: 'absolute', top, width: size }}><View style={[styles.laughLine, { left: 0, transform: [{ rotate: '-38deg' }], width: size * 0.62 }]} /><View style={[styles.laughLine, { right: 0, transform: [{ rotate: '38deg' }], width: size * 0.62 }]} /></View>; }
function CrossEye({ left, size, top }: { left: number; size: number; top: number }) { return <View style={{ height: size, left, position: 'absolute', top, width: size }}><View style={[styles.crossLine, { transform: [{ rotate: '45deg' }], width: size }]} /><View style={[styles.crossLine, { transform: [{ rotate: '-45deg' }], width: size }]} /></View>; }

const styles = StyleSheet.create({
  picker: { flexDirection: 'row', gap: space.xxs, justifyContent: 'space-between' }, option: { alignItems: 'center', borderRadius: radius.sm, flex: 1, gap: space.xxs, justifyContent: 'center', minHeight: 76, minWidth: 0, paddingHorizontal: 2, paddingVertical: space.xs }, optionSelected: { backgroundColor: palette.emotionSelected }, optionPressed: { opacity: .76, transform: [{ translateY: 1 }] }, optionLabel: { color: palette.muted, fontSize: 10, textAlign: 'center' }, optionLabelSelected: { color: palette.ink, fontWeight: '700' },
  face: { position: 'relative' }, eye: { backgroundColor: palette.emotionFaceInk, position: 'absolute' }, laughLine: { backgroundColor: palette.emotionFaceInk, borderRadius: 1, height: 2, position: 'absolute', top: 2 }, mouth: { position: 'absolute' }, crossLine: { backgroundColor: palette.emotionFaceInk, borderRadius: 1, height: 2, left: 0, position: 'absolute', top: 2 },
  chartRow: { flexDirection: 'row', gap: space.xs, minHeight: 212 }, axis: { height: PLOT_HEIGHT, position: 'relative', width: 68 }, axisLevel: { alignItems: 'center', flexDirection: 'row', justifyContent: 'flex-end', position: 'absolute', right: 0 }, axisMiddle: { transform: [{ translateY: -6 }] }, axisLast: { transform: [{ translateY: -12 }] }, axisLabel: { color: palette.muted, flexShrink: 1, fontSize: 9, lineHeight: 12 }, plotWrap: { flex: 1, minWidth: 0 }, plotArea: { height: PLOT_HEIGHT, minWidth: 0, position: 'relative' }, grid: { bottom: 0, justifyContent: 'space-between', left: 0, position: 'absolute', right: 0, top: 0 }, gridLine: { borderTopColor: palette.line, borderTopWidth: StyleSheet.hairlineWidth }, bars: { alignItems: 'flex-end', bottom: 0, flexDirection: 'row', gap: space.xxs, left: space.xxs, position: 'absolute', right: space.xxs, top: 0 }, barColumn: { alignItems: 'center', flex: 1, height: '100%', justifyContent: 'flex-end', minWidth: 0 }, bar: { borderRadius: radius.pill, minHeight: 4, width: 8 }, dayRow: { flexDirection: 'row', gap: space.xxs, height: 32, paddingHorizontal: space.xxs, paddingTop: space.xs }, day: { color: palette.muted, flex: 1, fontSize: 11, fontWeight: '700', minWidth: 0, textAlign: 'center' },
});

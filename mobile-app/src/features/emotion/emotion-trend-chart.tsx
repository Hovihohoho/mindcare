/* Hallmark · component: emotion trend chart · genre: modern-minimal · theme: MindCare
 * states: data · empty · loading · accessibility · mobile
 * contrast: pass · pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { optionFromScore } from './emotion.constants';
import type { EmotionTrendPoint } from './emotion.types';

const levels = [
  { score: 2, label: 'Rất vui' },
  { score: 1, label: 'Vui' },
  { score: 0, label: 'Bình thường' },
  { score: -1, label: 'Buồn' },
  { score: -2, label: 'Căng thẳng' },
] as const;

const SCORE_MIN = -2;
const SCORE_MAX = 2;
const SCORE_RANGE = SCORE_MAX - SCORE_MIN;
const PLOT_HEIGHT = 180;

function weekday(value: string) {
  const day = new Date(value).getDay();
  return day === 0 ? 'CN' : `T${day + 1}`;
}

function clampScore(score: number | null) {
  if (score === null || !Number.isFinite(score)) return SCORE_MIN;
  return Math.max(SCORE_MIN, Math.min(SCORE_MAX, score));
}

function heightFor(score: number) {
  return `${Math.max(3, ((score - SCORE_MIN) / SCORE_RANGE) * 100)}%` as `${number}%`;
}

export function EmotionTrendChart({ points }: { points: EmotionTrendPoint[] }) {
  if (!points.length) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyTitle}>Chưa có dữ liệu trong tuần</Text>
        <Text style={styles.emptyText}>Ghi lại cảm xúc đầu tiên để bắt đầu nhìn thấy xu hướng.</Text>
      </View>
    );
  }

  return (
    <View accessibilityLabel="Biểu đồ cảm xúc 7 ngày" style={styles.chartRow}>
      <View style={styles.axis}>
        {levels.map((level, index) => (
          <View
            key={level.score}
            style={[
              styles.axisLevel,
              { top: index * (PLOT_HEIGHT / (levels.length - 1)) },
              index > 0 && index < levels.length - 1 && styles.axisLevelMiddle,
              index === levels.length - 1 && styles.axisLevelLast,
            ]}
          >
            <Text numberOfLines={1} style={styles.axisLabel}>{level.label}</Text>
          </View>
        ))}
      </View>
      <View style={styles.plotWrap}>
        <View style={styles.plotArea}>
          <View pointerEvents="none" style={styles.grid}>
            {levels.map((level) => <View key={level.score} style={styles.gridLine} />)}
          </View>
          <View style={styles.bars}>
            {points.map((point) => {
              const score = clampScore(point.averageScore);
              const option = optionFromScore(score);
              const empty = point.averageScore === null || point.count === 0;
              const emotionLabel = option?.label ?? 'Căng thẳng';
              return (
                <View
                  accessibilityLabel={empty
                    ? `${weekday(point.periodStart)} chưa có dữ liệu`
                    : `${weekday(point.periodStart)}, ${emotionLabel}, ${point.count} bản ghi`}
                  key={point.periodStart}
                  style={styles.barColumn}
                >
                  <View style={[styles.bar, { backgroundColor: option?.color ?? colors.emotionStressed, height: heightFor(score) }]} />
                </View>
              );
            })}
          </View>
        </View>
        <View style={styles.dayRow}>
          {points.map((point) => (
            <Text key={point.periodStart} style={styles.day}>{weekday(point.periodStart)}</Text>
          ))}
        </View>
      </View>
    </View>
  );
}

export function EmotionTrendSkeleton() {
  return (
    <View accessibilityLabel="Đang tải biểu đồ cảm xúc" style={styles.skeleton}>
      {[38, 62, 45, 76, 58, 84, 68].map((height, index) => <View key={index} style={[styles.skeletonBar, { height: `${height}%` }]} />)}
    </View>
  );
}

const styles = StyleSheet.create({
  chartRow: { flexDirection: 'row', gap: spacing.xs, minHeight: 202 },
  axis: { height: PLOT_HEIGHT, position: 'relative', width: 68 },
  axisLevel: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs, justifyContent: 'flex-end', position: 'absolute', right: 0 },
  axisLevelMiddle: { transform: [{ translateY: -6 }] },
  axisLevelLast: { transform: [{ translateY: -12 }] },
  axisLabel: { color: colors.muted, flexShrink: 1, fontFamily: fonts.medium, fontSize: 9, lineHeight: 12 },
  plotWrap: { flex: 1, minWidth: 0 },
  plotArea: { height: PLOT_HEIGHT, minWidth: 0, position: 'relative' },
  grid: { bottom: 0, justifyContent: 'space-between', left: 0, position: 'absolute', right: 0, top: 0 },
  gridLine: { borderTopColor: colors.line, borderTopWidth: 1 },
  bars: { alignItems: 'flex-end', bottom: 0, flexDirection: 'row', gap: spacing.xxs, left: spacing.xxs, position: 'absolute', right: spacing.xxs, top: 0 },
  barColumn: { alignItems: 'center', flex: 1, height: '100%', justifyContent: 'flex-end', minWidth: 0 },
  bar: { borderRadius: radius.pill, minHeight: 4, width: 8 },
  dayRow: { flexDirection: 'row', gap: spacing.xxs, height: 32, paddingHorizontal: spacing.xxs, paddingTop: spacing.xs },
  day: { color: colors.muted, flex: 1, fontFamily: fonts.semibold, fontSize: type.tab, minWidth: 0, textAlign: 'center' },
  empty: { alignItems: 'center', justifyContent: 'center', minHeight: 156, padding: spacing.lg },
  emptyTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.label, textAlign: 'center' },
  emptyText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xs, textAlign: 'center' },
  skeleton: { alignItems: 'flex-end', flexDirection: 'row', gap: spacing.xs, height: 180, paddingHorizontal: spacing.sm },
  skeletonBar: { backgroundColor: colors.skeleton, borderRadius: radius.pill, flex: 1, maxWidth: 8 },
});

/* Hallmark · component: emotion calendar · genre: modern-minimal · theme: MindCare
 * states: data · empty · selected · pressed · disabled · loading · error
 * contrast: pass · mobile: 360–430 dp · pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';
import { emotionOptions, optionFromScore } from './emotion.constants';
import { EmotionFaceIcon } from './emotion-face';
import type { EmotionTrendPoint } from './emotion.types';

const weekdays = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

type Props = {
  month: Date;
  points: EmotionTrendPoint[];
  selectedDate: Date;
  onSelectDate(date: Date): void;
  onChangeMonth(offset: number): void;
  canGoNext: boolean;
};

export function EmotionCalendar({ month, points, selectedDate, onSelectDate, onChangeMonth, canGoNext }: Props) {
  const year = month.getFullYear();
  const monthIndex = month.getMonth();
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const leading = (new Date(year, monthIndex, 1).getDay() + 6) % 7;
  const pointsByDate = new Map(points.map((point) => [point.periodStart.slice(0, 10), point]));
  const cells = Array.from({ length: leading + daysInMonth }, (_, index) => {
    const day = index - leading + 1;
    return day > 0 ? day : null;
  });
  const todayKey = dateKey(new Date());

  return (
    <View accessibilityLabel="Lịch cảm xúc theo tháng" style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>Góc nhìn tổng quan</Text>
        <View style={styles.monthControl}>
          <MonthButton icon="chevron-back" label="Tháng trước" onPress={() => onChangeMonth(-1)} />
          <Text numberOfLines={1} style={styles.monthLabel}>Tháng {monthIndex + 1}/{year}</Text>
          <MonthButton disabled={!canGoNext} icon="chevron-forward" label="Tháng sau" onPress={() => onChangeMonth(1)} />
        </View>
      </View>

      <View style={styles.weekRow}>
        {weekdays.map((item) => <Text key={item} style={styles.weekday}>{item}</Text>)}
      </View>
      <View style={styles.grid}>
        {cells.map((day, index) => {
          if (day === null) return <View key={`empty-${index}`} style={styles.cellSlot} />;
          const date = new Date(year, monthIndex, day);
          const key = dateKey(date);
          const point = pointsByDate.get(key);
          const option = optionFromScore(point?.averageScore ?? null);
          const selected = dateKey(selectedDate) === key;
          const future = key > todayKey;
          return (
            <View key={key} style={styles.cellSlot}>
              <Pressable
                accessibilityLabel={`${day}/${monthIndex + 1}/${year}${option ? `, ${option.label}, ${point?.count ?? 0} bản ghi` : ', chưa có nhật ký'}`}
                accessibilityRole="button"
                accessibilityState={{ disabled: future, selected }}
                disabled={future}
                onPress={() => onSelectDate(date)}
                style={({ pressed }) => [styles.dayCell, selected && styles.dayCellSelected, pressed && styles.dayCellPressed, future && styles.dayCellDisabled]}
              >
                <Text style={[styles.dayNumber, selected && styles.dayNumberSelected]}>{String(day).padStart(2, '0')}</Text>
                {option ? <EmotionFaceIcon color={option.color} face={option.face} size={22} /> : <Text style={styles.emptyMark}>–</Text>}
                {selected ? <View style={styles.selectedDot} /> : null}
              </Pressable>
            </View>
          );
        })}
      </View>
      <View style={styles.legend}>
        {emotionOptions.map((option) => (
          <View key={option.value} style={styles.legendItem}><View style={[styles.legendDot, { backgroundColor: option.color }]} /><Text style={styles.legendText}>{option.label}</Text></View>
        ))}
      </View>
    </View>
  );
}

export function EmotionCalendarSkeleton() {
  return (
    <View accessibilityLabel="Đang tải lịch cảm xúc" style={styles.card}>
      <View style={styles.skeletonHeader}><View style={styles.skeletonTitle} /><View style={styles.skeletonMonth} /></View>
      <View style={styles.skeletonGrid}>{Array.from({ length: 35 }, (_, index) => <View key={index} style={styles.skeletonCell} />)}</View>
    </View>
  );
}

function MonthButton({ icon, label, onPress, disabled = false }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress(): void; disabled?: boolean }) {
  return (
    <Pressable accessibilityLabel={label} accessibilityRole="button" accessibilityState={{ disabled }} disabled={disabled} hitSlop={4} onPress={onPress} style={({ pressed }) => [styles.monthButton, pressed && styles.monthButtonPressed, disabled && styles.monthButtonDisabled]}>
      <Ionicons color={colors.inkSoft} name={icon} size={17} />
    </Pressable>
  );
}

export function dateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const styles = StyleSheet.create({
  card: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginHorizontal: -spacing.md, overflow: 'hidden', ...shadows.card },
  header: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs, justifyContent: 'space-between', padding: spacing.sm },
  title: { color: colors.ink, flex: 1, fontFamily: fonts.bold, fontSize: type.cardTitle },
  monthControl: { alignItems: 'center', flexDirection: 'row' },
  monthButton: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', width: 44 },
  monthButtonPressed: { backgroundColor: colors.surfacePressed },
  monthButtonDisabled: { opacity: 0.35 },
  monthLabel: { color: colors.inkSoft, fontFamily: fonts.semibold, fontSize: type.caption, minWidth: 82, textAlign: 'center' },
  weekRow: { flexDirection: 'row' },
  weekday: { color: colors.muted, fontFamily: fonts.bold, fontSize: type.tab, paddingBottom: spacing.xs, textAlign: 'center', width: `${100 / 7}%` },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  cellSlot: { aspectRatio: 0.88, width: `${100 / 7}%` },
  dayCell: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.sm, borderWidth: 1, flex: 1, justifyContent: 'center', minHeight: 44, position: 'relative' },
  dayCellSelected: { backgroundColor: colors.surfaceMuted, borderColor: colors.brandDark },
  dayCellPressed: { backgroundColor: colors.surfacePressed },
  dayCellDisabled: { backgroundColor: colors.canvas, opacity: 0.42 },
  dayNumber: { color: colors.muted, fontFamily: fonts.bold, fontSize: 8, left: spacing.xxs, position: 'absolute', top: spacing.xxs },
  dayNumberSelected: { color: colors.brandDark },
  emptyMark: { color: colors.lineStrong, fontFamily: fonts.semibold, fontSize: type.body },
  selectedDot: { backgroundColor: colors.brandDark, borderRadius: radius.pill, bottom: 4, height: 4, position: 'absolute', width: 4 },
  legend: { borderTopColor: colors.line, borderTopWidth: 1, flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, justifyContent: 'center', marginTop: spacing.sm, padding: spacing.sm },
  legendItem: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs },
  legendDot: { borderRadius: radius.pill, height: 8, width: 8 },
  legendText: { color: colors.muted, fontFamily: fonts.regular, fontSize: 9 },
  skeletonHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', padding: spacing.md },
  skeletonTitle: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 18, width: '44%' },
  skeletonMonth: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 18, width: '28%' },
  skeletonGrid: { flexDirection: 'row', flexWrap: 'wrap' },
  skeletonCell: { aspectRatio: 0.88, backgroundColor: colors.skeleton, borderColor: colors.surface, borderWidth: 2, width: `${100 / 7}%` },
});

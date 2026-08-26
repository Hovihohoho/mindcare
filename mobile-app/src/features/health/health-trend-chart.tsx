/* Hallmark · component: health trend chart · genre: modern-minimal · theme: MindCare
 * states: data · empty · accessibility · responsive
 */
import { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import type { HealthTrendPoint } from './health.types';

const PLOT_HEIGHT = 140;

function weekday(value: string) {
  const day = new Date(value).getDay();
  return day === 0 ? 'CN' : `T${day + 1}`;
}

function compact(value: number) {
  if (value >= 1_000) return `${Math.round(value / 1_000)}k`;
  return Math.round(value).toLocaleString('vi-VN');
}

export function HealthTrendChart({ points }: { points: HealthTrendPoint[] }) {
  const [width, setWidth] = useState(0);
  const data = useMemo(() => points.slice(-7), [points]);
  const maximum = Math.max(...data.map((point) => point.value), 0);
  const axisMaximum = Math.max(1_000, Math.ceil(maximum / 1_000) * 1_000);
  const average = data.length ? data.reduce((sum, point) => sum + point.value, 0) / data.length : 0;

  if (!data.length) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyTitle}>Chưa có dữ liệu xu hướng</Text>
        <Text style={styles.emptyText}>Đồng bộ Health Connect để xem thay đổi theo ngày.</Text>
      </View>
    );
  }

  const coordinates = data.map((point, index) => ({
    point,
    x: data.length === 1 ? width / 2 : (index / (data.length - 1)) * width,
    y: PLOT_HEIGHT - 8 - (point.value / axisMaximum) * (PLOT_HEIGHT - 16),
  }));

  return (
    <View accessibilityLabel="Biểu đồ bước chân trong 7 ngày">
      <View style={styles.chartRow}>
        <View style={styles.axis}>
          <Text style={styles.axisLabel}>{compact(axisMaximum)}</Text>
          <Text style={styles.axisLabel}>{compact(axisMaximum / 2)}</Text>
          <Text style={styles.axisLabel}>0</Text>
        </View>
        <View style={styles.plotColumn}>
          <View onLayout={(event) => setWidth(event.nativeEvent.layout.width)} style={styles.plot}>
            <View style={[styles.gridLine, styles.gridTop]} />
            <View style={[styles.gridLine, styles.gridMiddle]} />
            <View style={[styles.gridLine, styles.gridBottom]} />
            {width > 0 ? coordinates.slice(0, -1).map((coordinate, index) => {
              const next = coordinates[index + 1];
              const dx = next.x - coordinate.x;
              const dy = next.y - coordinate.y;
              const length = Math.sqrt(dx * dx + dy * dy);
              const angle = Math.atan2(dy, dx) * (180 / Math.PI);
              return (
                <View
                  key={`line-${coordinate.point.periodStart}`}
                  style={[styles.line, { left: (coordinate.x + next.x - length) / 2, top: (coordinate.y + next.y) / 2, width: length, transform: [{ rotate: `${angle}deg` }] }]}
                />
              );
            }) : null}
            {width > 0 ? coordinates.map((coordinate) => (
              <View
                accessibilityLabel={`${weekday(coordinate.point.periodStart)}: ${Math.round(coordinate.point.value).toLocaleString('vi-VN')} bước`}
                key={coordinate.point.periodStart}
                style={[styles.dot, { left: coordinate.x - 4, top: coordinate.y - 4 }]}
              />
            )) : null}
          </View>
          <View style={styles.dayRow}>
            {data.map((point) => <Text key={point.periodStart} style={styles.day}>{weekday(point.periodStart)}</Text>)}
          </View>
        </View>
      </View>
      <Text style={styles.average}>Trung bình: <Text style={styles.averageValue}>{Math.round(average).toLocaleString('vi-VN')} bước/ngày</Text></Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chartRow: { flexDirection: 'row', gap: spacing.xs },
  axis: { height: PLOT_HEIGHT, justifyContent: 'space-between', paddingBottom: 2, width: 34 },
  axisLabel: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: 10, textAlign: 'right' },
  plotColumn: { flex: 1, minWidth: 0 },
  plot: { height: PLOT_HEIGHT, position: 'relative' },
  gridLine: { borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, left: 0, position: 'absolute', right: 0 },
  gridTop: { top: 0 },
  gridMiddle: { top: '50%' },
  gridBottom: { bottom: 0 },
  line: { backgroundColor: colors.brand, height: 2, position: 'absolute' },
  dot: { backgroundColor: colors.surface, borderColor: colors.brand, borderRadius: radius.pill, borderWidth: 2, height: 8, position: 'absolute', width: 8 },
  dayRow: { flexDirection: 'row', paddingTop: spacing.xs },
  day: { color: colors.tertiary, flex: 1, fontFamily: fonts.medium, fontSize: type.tab, textAlign: 'center' },
  average: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.md },
  averageValue: { color: colors.ink, fontFamily: fonts.medium },
  empty: { alignItems: 'center', justifyContent: 'center', minHeight: 164, padding: spacing.lg },
  emptyTitle: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.label },
  emptyText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xs, textAlign: 'center' },
});

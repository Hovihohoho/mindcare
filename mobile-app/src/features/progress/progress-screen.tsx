/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * genre: modern-minimal · macrostructure: Native Content Flow · design-system: design.md
 */
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { SectionHeader } from '@/components/section-header';
import { EmotionTrendChart } from '@/features/emotion/emotion-trend-chart';
import { useAuth } from '@/features/auth/auth-context';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { progressService, type ProgressData } from './progress.service';

function total(points: { value: number }[] | undefined) {
  return points?.reduce((sum, point) => sum + point.value, 0) ?? 0;
}

function format(value: number, digits = 0) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: digits }).format(value);
}

export function ProgressScreen() {
  const { session } = useAuth();
  const router = useRouter();
  const [data, setData] = useState<ProgressData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    if (!session?.accessToken) return;
    setError(false);
    try {
      setData(await progressService.get(session.accessToken));
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [session?.accessToken]);

  useEffect(() => { void load(); }, [load]);

  const steps = total(data?.currentHealth.STEP_COUNT);
  const sleep = total(data?.currentHealth.SLEEP_SESSION);
  const exercise = total(data?.currentHealth.EXERCISE_SESSION);
  const trendItems = [
    { label: 'Giấc ngủ', value: sleep ? `${format(sleep / 60, 1)} giờ` : 'Chưa có', detail: 'Tổng thời lượng trong 7 ngày gần nhất' },
    { label: 'Hoạt động', value: `${format(steps)} bước`, detail: exercise ? `${format(exercise)} phút vận động đã đồng bộ` : 'Chưa có dữ liệu vận động' },
    { label: 'Check-in cảm xúc', value: `${format(data?.currentCheckIns ?? 0)} lần`, detail: data?.previousCheckIns ? `Tuần trước: ${format(data.previousCheckIns)} lần` : 'Chưa đủ dữ liệu để so sánh' },
  ];

  return (
    <AppScreen>
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl onRefresh={() => { setLoading(true); void load(); }} refreshing={loading} tintColor={colors.brand} />}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.date}>7 NGÀY GẦN NHẤT</Text>
        <Text accessibilityRole="header" style={styles.title}>Theo dõi sức khỏe của bạn</Text>
        <Text style={styles.intro}>Nhìn vào các thay đổi theo thời gian, thay vì đánh giá một ngày đơn lẻ.</Text>

        {error ? <View style={styles.notice}><Text style={styles.noticeTitle}>Chưa tải được tiến trình</Text><Text style={styles.noticeText}>Kéo xuống để thử lại. Dữ liệu gốc của bạn không bị ảnh hưởng.</Text></View> : null}

        <View style={styles.rule} />
        <SectionHeader actionLabel="Xem lịch" onAction={() => router.push('/(tabs)/journal-history')} title="Cảm xúc" />
        <View style={styles.chartCard}>
          <EmotionTrendChart points={data?.emotionTrends ?? []} />
          <Text style={styles.caption}>{data?.currentCheckIns ?? 0} check-in trong tuần · Dữ liệu giúp bạn theo dõi, không dùng để chẩn đoán.</Text>
        </View>

        <View style={styles.rule} />
        <SectionHeader title="Xu hướng sức khỏe" />
        <View style={styles.list}>
          {trendItems.map((item, index) => <Trend key={item.label} {...item} last={index === trendItems.length - 1} />)}
        </View>
        <Text style={styles.note}>Các xu hướng chỉ hỗ trợ bạn theo dõi sức khỏe, không thay thế tư vấn y tế.</Text>
      </ScrollView>
    </AppScreen>
  );
}

function Trend({ label, value, detail, last }: { label: string; value: string; detail: string; last: boolean }) {
  return <View style={[styles.trend, !last && styles.trendRule]}><Text style={styles.trendLabel}>{label}</Text><Text style={styles.trendValue}>{value}</Text><Text style={styles.trendDetail}>{detail}</Text></View>;
}

const styles = StyleSheet.create({
  content: { padding: spacing.page, paddingBottom: 36 },
  date: { color: colors.muted, fontFamily: fonts.semibold, fontSize: type.caption, letterSpacing: .7 },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, letterSpacing: -.8, lineHeight: 38, marginTop: 4 },
  intro: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23, marginTop: 10, maxWidth: 350 },
  rule: { backgroundColor: colors.line, height: StyleSheet.hairlineWidth, marginVertical: spacing.xl },
  chartCard: { backgroundColor: colors.brandSoft, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginTop: spacing.md, padding: spacing.md },
  caption: { borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.md, paddingTop: spacing.md },
  list: { backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginTop: spacing.md },
  trend: { padding: spacing.md },
  trendRule: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth },
  trendLabel: { color: colors.muted, fontFamily: fonts.semibold, fontSize: type.caption },
  trendValue: { color: colors.ink, fontFamily: fonts.bold, fontSize: 17, marginTop: 6 },
  trendDetail: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: 4 },
  note: { color: colors.muted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 16, marginTop: spacing.md },
  notice: { backgroundColor: colors.warningSoft, borderColor: colors.line, borderLeftColor: colors.warning, borderLeftWidth: 3, borderRadius: radius.card, borderWidth: 1, marginTop: spacing.lg, padding: spacing.md },
  noticeTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.label },
  noticeText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 4 },
});

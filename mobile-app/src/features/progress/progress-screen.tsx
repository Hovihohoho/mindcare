import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { useAuth } from '@/features/auth/auth-context';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { progressService, type ProgressData } from './progress.service';

function total(points: { value: number }[] | undefined) { return points?.reduce((sum, point) => sum + point.value, 0) ?? 0; }
function format(value: number, digits = 0) { return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: digits }).format(value); }

export function ProgressScreen() {
  const { session } = useAuth();
  const router = useRouter();
  const [data, setData] = useState<ProgressData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const load = useCallback(async () => {
    if (!session?.accessToken) return;
    setError(false);
    try { setData(await progressService.get(session.accessToken)); }
    catch { setError(true); }
    finally { setLoading(false); }
  }, [session?.accessToken]);
  useEffect(() => { void load(); }, [load]);

  const steps = total(data?.currentHealth.STEP_COUNT);
  const sleep = total(data?.currentHealth.SLEEP_SESSION);
  const exercise = total(data?.currentHealth.EXERCISE_SESSION);
  const planPercent = data?.carePlan?.targetThisWeek ? Math.min(100, Math.round(data.carePlan.completedThisWeek * 100 / data.carePlan.targetThisWeek)) : 0;
  const items = [
    { label: 'Lần check-in', value: format(data?.currentCheckIns ?? 0), icon: 'heart-outline' as const },
    { label: 'Bước chân', value: format(steps), icon: 'footsteps-outline' as const },
    { label: 'Thời lượng ngủ', value: sleep ? `${format(sleep / 60, 1)} giờ` : 'Chưa có', icon: 'moon-outline' as const },
    { label: 'Vận động', value: exercise ? `${format(exercise)} phút` : 'Chưa có', icon: 'fitness-outline' as const },
  ];

  return <AppScreen><ScrollView contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={loading} onRefresh={() => { setLoading(true); void load(); }} tintColor={colors.brand} />}>
    <Text style={styles.eyebrow}>7 NGÀY GẦN NHẤT</Text><Text style={styles.title}>Tiến triển của bạn</Text><Text style={styles.description}>Một góc nhìn nhẹ nhàng từ dữ liệu bạn đã ghi nhận. Đây không phải chẩn đoán hay điểm số sức khỏe.</Text>
    {error && <View style={styles.notice}><Text style={styles.cardTitle}>Chưa tải được tiến triển</Text><Text style={styles.body}>Kéo xuống để thử lại. Dữ liệu gốc của bạn không bị ảnh hưởng.</Text></View>}
    {!error && <><View style={styles.grid}>{items.map((item) => <View style={styles.metric} key={item.label}><View style={styles.metricHead}><Text style={styles.label}>{item.label}</Text><Ionicons color={colors.brand} name={item.icon} size={20} /></View><Text style={styles.value}>{item.value}</Text><Text style={styles.caption}>Trong tuần này</Text></View>)}</View>
    <Pressable style={styles.card} onPress={() => router.push('/(tabs)/journal')}><View style={styles.cardHead}><View><Text style={styles.labelBrand}>CHECK-IN CẢM XÚC</Text><Text style={styles.cardTitle}>{data?.currentCheckIns ?? 0} lần trong tuần</Text></View><Ionicons color={colors.brand} name="chevron-forward" size={22} /></View><Text style={styles.body}>{data?.previousCheckIns ? `Tuần trước bạn đã check-in ${data.previousCheckIns} lần.` : 'Chưa đủ dữ liệu tuần trước để so sánh.'}</Text></Pressable>
    <View style={styles.card}><Text style={styles.labelBrand}>KẾ HOẠCH TỰ CHĂM SÓC</Text><Text style={styles.cardTitle}>{data?.carePlan ? `${data.carePlan.completedThisWeek}/${data.carePlan.targetThisWeek} hoạt động` : 'Chưa thiết lập kế hoạch'}</Text>{data?.carePlan && <><View style={styles.track}><View style={[styles.progress, { width: `${planPercent}%` }]} /></View><Text style={styles.body}>Bạn đã hoàn thành {planPercent}% mục tiêu tuần này.</Text></>}</View>
    <Pressable style={styles.card} onPress={() => router.push('/(tabs)/assessments')}><View style={styles.cardHead}><View style={styles.flex}><Text style={styles.labelBrand}>SÀNG LỌC GẦN NHẤT</Text><Text style={styles.cardTitle}>{data?.latestAssessment ? `${data.latestAssessment.assessmentCode} · ${data.latestAssessment.riskLevel.replaceAll('_', ' ')}` : 'Chưa có kết quả'}</Text></View><Ionicons color={colors.brand} name="chevron-forward" size={22} /></View><Text style={styles.body}>{data?.latestAssessment ? `Thực hiện ngày ${new Date(data.latestAssessment.createdAt).toLocaleDateString('vi-VN')}. Kết quả chỉ mang ý nghĩa sàng lọc và theo dõi.` : 'Bạn có thể thực hiện một bài sàng lọc khi cảm thấy sẵn sàng.'}</Text></Pressable></>}
  </ScrollView></AppScreen>;
}

const styles = StyleSheet.create({
  content: { padding: spacing.page, paddingBottom: spacing.xxl, gap: spacing.md },
  eyebrow: { color: colors.brand, fontFamily: fonts.bold, fontSize: type.caption, letterSpacing: 1.4 },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, lineHeight: 38 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23, marginBottom: spacing.xs },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  metric: { width: '48%', flexGrow: 1, minWidth: 140, backgroundColor: colors.surface, borderColor: colors.line, borderWidth: 1, borderRadius: radius.card, padding: spacing.md },
  metricHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: spacing.xs },
  label: { color: colors.muted, fontFamily: fonts.medium, fontSize: type.label },
  value: { color: colors.ink, fontFamily: fonts.bold, fontSize: 22, marginTop: spacing.md },
  caption: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xxs },
  card: { backgroundColor: colors.surface, borderColor: colors.line, borderWidth: 1, borderRadius: radius.card, padding: spacing.md, gap: spacing.sm },
  notice: { backgroundColor: colors.warningSoft, borderRadius: radius.card, padding: spacing.md, gap: spacing.xs },
  cardHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  flex: { flex: 1 },
  labelBrand: { color: colors.brand, fontFamily: fonts.bold, fontSize: type.caption, letterSpacing: 0.7 },
  cardTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.cardTitle, marginTop: spacing.xxs },
  body: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20 },
  track: { height: 9, borderRadius: radius.pill, backgroundColor: colors.surfaceMuted, overflow: 'hidden', marginTop: spacing.xs },
  progress: { height: '100%', borderRadius: radius.pill, backgroundColor: colors.brand },
});

import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Linking, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ActionButton } from '@/components/buttons';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { useAuth } from '@/features/auth/auth-context';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { carePlanService, type CarePlan, type CarePlanRecommendation, type CarePlanTemplate } from './care-plan.service';

const goalLabels = { REDUCE_STRESS: 'Giảm căng thẳng', IMPROVE_SLEEP: 'Ngủ tốt hơn', MANAGE_ANXIETY: 'Quản lý lo âu', BUILD_BALANCE: 'Xây dựng cân bằng' } as const;

function localDate() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export default function CarePlanScreen() {
  const { session } = useAuth();
  const router = useRouter();
  const [plan, setPlan] = useState<CarePlan | null>(null);
  const [templates, setTemplates] = useState<CarePlanTemplate[]>([]);
  const [recommendations, setRecommendations] = useState<CarePlanRecommendation[]>([]);
  const [selectedCode, setSelectedCode] = useState<string>();
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!session?.accessToken) return;
    setError('');
    try {
      const [nextPlan, nextTemplates, nextRecommendations] = await Promise.all([
        carePlanService.get(session.accessToken), carePlanService.templates(session.accessToken), carePlanService.recommendations(session.accessToken),
      ]);
      setPlan(nextPlan); setTemplates(nextTemplates); setRecommendations(nextRecommendations);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Không thể tải kế hoạch tự chăm sóc.');
    } finally { setLoading(false); }
  }, [session?.accessToken]);

  useEffect(() => { void load(); }, [load]);
  const recommendedCodes = useMemo(() => new Set(recommendations.map((item) => item.templateCode)), [recommendations]);
  const selected = templates.find((item) => item.templateCode === (selectedCode ?? recommendations[0]?.templateCode ?? plan?.templateCode)) ?? templates[0];
  const progress = plan?.targetThisWeek ? Math.min(100, Math.round(plan.completedThisWeek * 100 / plan.targetThisWeek)) : 0;

  const apply = async () => {
    if (!session?.accessToken || !selected || saving) return;
    setSaving(true);
    try { setPlan(await carePlanService.applyTemplate(session.accessToken, selected.templateCode)); setEditing(false); }
    catch (caught) { setError(caught instanceof Error ? caught.message : 'Không thể áp dụng kế hoạch.'); }
    finally { setSaving(false); }
  };
  const toggle = async (activityId: string, completed: boolean) => {
    if (!session?.accessToken || saving) return;
    setSaving(true);
    try { setPlan(completed ? await carePlanService.undo(session.accessToken, activityId, localDate()) : await carePlanService.complete(session.accessToken, activityId, localDate())); }
    catch (caught) { setError(caught instanceof Error ? caught.message : 'Không thể cập nhật hoạt động.'); }
    finally { setSaving(false); }
  };

  return <AppScreen>{loading ? <SkeletonList rows={4} /> : error && !plan ? <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => { setLoading(true); void load(); }} title="Chưa tải được kế hoạch" /> : (
    <ScrollView contentContainerStyle={styles.content} refreshControl={<RefreshControl onRefresh={() => { setLoading(true); void load(); }} refreshing={loading} tintColor={colors.brand} />} showsVerticalScrollIndicator={false}>
      <Text style={styles.eyebrow}>TỰ CHĂM SÓC</Text><Text accessibilityRole="header" style={styles.title}>Kế hoạch tự chăm sóc</Text>
      <Text style={styles.intro}>Chọn một kế hoạch đã được kiểm duyệt và thực hiện theo nhịp phù hợp với bạn.</Text>
      <ActionButton icon="clipboard-outline" label="Thực hiện bài đánh giá" onPress={() => router.push('/assessments')} tone="secondary" />
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {plan && !editing ? <>
        <View style={styles.progressCard}><View style={styles.progressTop}><View><Text style={styles.muted}>Tiến độ tuần này · {goalLabels[plan.goal]}</Text><Text style={styles.progressValue}>{plan.completedThisWeek}/{plan.targetThisWeek}</Text></View><Text style={styles.percent}>{progress}%</Text></View><View style={styles.track}><View style={[styles.fill, { width: `${progress}%` }]} /></View>{plan.sourceUrl ? <Pressable onPress={() => void Linking.openURL(plan.sourceUrl!)} style={styles.source}><Text style={styles.sourceText}>Nguồn của kế hoạch</Text><Ionicons color={colors.brand} name="open-outline" size={15} /></Pressable> : null}</View>
        <View style={styles.activities}>{plan.activities.map((item) => <View key={item.id} style={styles.activity}><View style={styles.activityCopy}><Text style={styles.activityTitle}>{item.title}</Text><Text style={styles.activityDetail}>Đã làm {item.completedThisWeek}/{item.targetPerWeek} lần trong tuần</Text></View><Pressable accessibilityLabel={item.completedToday ? 'Bỏ đánh dấu hôm nay' : 'Đánh dấu hoàn thành hôm nay'} disabled={saving} onPress={() => void toggle(item.id, item.completedToday)} style={[styles.check, item.completedToday && styles.checked]}>{saving ? <ActivityIndicator color={colors.accentInk} size="small" /> : <Ionicons color={item.completedToday ? colors.accentInk : colors.tertiary} name={item.completedToday ? 'checkmark' : 'ellipse-outline'} size={23} />}</Pressable></View>)}</View>
        <ActionButton label="Thiết lập lại kế hoạch" onPress={() => setEditing(true)} tone="secondary" />
      </> : <>
        {recommendations.map((item) => <View key={item.templateCode} style={styles.recommendation}><Ionicons color={colors.warning} name="sparkles-outline" size={20} /><Text style={styles.recommendationText}>{item.message}</Text></View>)}
        {templates.map((item) => <Pressable key={item.templateCode} onPress={() => setSelectedCode(item.templateCode)} style={[styles.template, selected?.templateCode === item.templateCode && styles.templateSelected]}><View style={styles.templateTop}><Ionicons color={colors.brand} name="flag-outline" size={23} />{recommendedCodes.has(item.templateCode) ? <Text style={styles.badge}>Được đề xuất</Text> : null}</View><Text style={styles.templateTitle}>{item.title}</Text><Text style={styles.templateText}>{item.description}</Text><Text style={styles.templateMeta}>{item.activities.length} hoạt động · phiên bản {item.templateVersion}</Text></Pressable>)}
        {selected ? <View style={styles.detail}><Text style={styles.detailTitle}>Hoạt động trong kế hoạch</Text>{selected.activities.map((item) => <View key={item.activityCode} style={styles.detailRow}><Text style={styles.detailText}>{item.title}</Text><Text style={styles.detailTarget}>{item.targetPerWeek} lần/tuần</Text></View>)}<Text style={styles.limitation}>{selected.limitation}</Text><ActionButton label="Áp dụng kế hoạch" loading={saving} onPress={() => void apply()} /></View> : <DataFeedback kind="empty" title="Chưa có kế hoạch mẫu" description="Kế hoạch sẽ hiển thị khi được hệ thống cung cấp." />}
      </>}
    </ScrollView>
  )}</AppScreen>;
}

const styles = StyleSheet.create({
  content: { gap: spacing.md, padding: spacing.page, paddingBottom: 40 }, eyebrow: { color: colors.muted, fontFamily: fonts.semibold, fontSize: type.caption, letterSpacing: .7 }, title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, letterSpacing: -.8, lineHeight: 38 }, intro: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23, marginBottom: spacing.xs }, error: { color: colors.danger, fontFamily: fonts.medium, fontSize: type.caption }, progressCard: { backgroundColor: colors.brandSoft, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.md, padding: spacing.md }, progressTop: { alignItems: 'flex-end', flexDirection: 'row', justifyContent: 'space-between' }, muted: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label }, progressValue: { color: colors.ink, fontFamily: fonts.bold, fontSize: 30, marginTop: spacing.xs }, percent: { color: colors.brandDark, fontFamily: fonts.bold, fontSize: type.cardTitle }, track: { backgroundColor: colors.surface, borderRadius: radius.pill, height: 10, overflow: 'hidden' }, fill: { backgroundColor: colors.brand, borderRadius: radius.pill, height: '100%' }, source: { alignItems: 'center', alignSelf: 'flex-start', flexDirection: 'row', gap: spacing.xxs, minHeight: 30 }, sourceText: { color: colors.brand, fontFamily: fonts.semibold, fontSize: type.caption }, activities: { backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, overflow: 'hidden' }, activity: { alignItems: 'center', borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.md, minHeight: 82, padding: spacing.md }, activityCopy: { flex: 1 }, activityTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body }, activityDetail: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: 3 }, check: { alignItems: 'center', backgroundColor: colors.surface, borderRadius: radius.pill, height: 44, justifyContent: 'center', width: 44 }, checked: { backgroundColor: colors.brand }, recommendation: { alignItems: 'flex-start', backgroundColor: colors.warningSoft, borderColor: colors.line, borderLeftColor: colors.warning, borderLeftWidth: 3, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, padding: spacing.md }, recommendationText: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20 }, template: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.xs, padding: spacing.md }, templateSelected: { backgroundColor: colors.brandSoft, borderColor: colors.brand, borderWidth: 2 }, templateTop: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' }, badge: { backgroundColor: colors.warningSoft, borderRadius: radius.pill, color: colors.warning, fontFamily: fonts.semibold, fontSize: 11, paddingHorizontal: spacing.xs, paddingVertical: spacing.xxs }, templateTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle }, templateText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20 }, templateMeta: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption }, detail: { backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.sm, padding: spacing.md }, detailTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle }, detailRow: { flexDirection: 'row', gap: spacing.sm, justifyContent: 'space-between' }, detailText: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.label }, detailTarget: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption }, limitation: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginVertical: spacing.xs },
});

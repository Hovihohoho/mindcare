import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { assessmentService, type AssessmentSummary } from '@/services/assessment/assessment.service';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type AssessmentItem = AssessmentSummary & { completed: boolean; questionCount: number };

const questionCounts: Record<string, number> = { 'PHQ-9': 9, 'GAD-7': 7, 'DASS-21': 21, 'PSS-10': 10, 'WHO-5': 5 };

async function loadAssessments(token: string): Promise<AssessmentItem[]> {
  const to = new Date();
  const from = new Date(to);
  from.setFullYear(from.getFullYear() - 1);
  const [items, history] = await Promise.all([
    assessmentService.list(token),
    assessmentService.history(token, from.toISOString(), to.toISOString()),
  ]);
  const completedCodes = new Set(history.items.map((item) => item.assessmentCode));
  return items.map((item) => ({ ...item, completed: completedCodes.has(item.code), questionCount: questionCounts[item.code] ?? 0 }));
}

export default function AssessmentsScreen() {
  const router = useRouter();
  const { data, loading, refreshing, error, reload } = useAuthenticatedList(loadAssessments);

  return (
    <AppScreen>
      {loading ? <SkeletonList rows={3} /> : error ? (
        <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Thư viện chưa được tải" />
      ) : data.length === 0 ? (
        <DataFeedback description="Các bài đánh giá sẽ xuất hiện tại đây khi sẵn sàng." kind="empty" title="Chưa có bài đánh giá" />
      ) : (
        <FlatList
          data={data}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
          ListHeaderComponent={<View style={styles.pageIntro}><Text style={styles.eyebrow}>TỰ ĐÁNH GIÁ</Text><Text accessibilityRole="header" style={styles.pageTitle}>Hiểu rõ hơn điều bạn đang trải qua</Text><Text style={styles.intro}>Các bảng hỏi chỉ giúp bạn tự nhìn nhận, không thay thế chẩn đoán chuyên môn.</Text><View style={styles.rule} /></View>}
          renderItem={({ item, index }) => <AssessmentItemRow first={index === 0} item={item} last={index === data.length - 1} onPress={() => router.push({ pathname: '/assessment/[code]', params: { code: item.code } })} />}
          ListFooterComponent={(
            <View style={styles.disclaimer}>
              <Ionicons color={colors.muted} name="information-circle-outline" size={19} />
              <Text style={styles.disclaimerText}>Nguồn, phiên bản và giới hạn sử dụng được hiển thị trước khi bạn bắt đầu.</Text>
            </View>
          )}
        />
      )}
    </AppScreen>
  );
}

function AssessmentItemRow({ item, onPress, first, last }: { item: AssessmentItem; onPress(): void; first: boolean; last: boolean }) {
  return (
    <Pressable accessibilityLabel={`${item.title}, ${item.questionCount} câu`} accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.item, first && styles.itemFirst, last && styles.itemLast, !last && styles.itemDivider, pressed && styles.itemPressed]}>
      <View style={styles.body}>
        <View style={styles.titleRow}>
          <Text style={styles.code}>{item.code}</Text>
          {item.completed ? <Text style={styles.doneText}>Đã thực hiện</Text> : null}
        </View>
        <Text style={styles.title}>{item.title}</Text>
        <Text numberOfLines={2} style={styles.description}>{item.description}</Text>
        <Text style={styles.metaText}>{item.questionCount} câu · khoảng 5 phút</Text>
      </View>
      <Ionicons color={colors.tertiary} name="chevron-forward" size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  list: { paddingHorizontal: spacing.page, paddingBottom: 96 },
  pageIntro: { paddingTop: spacing.sm },
  eyebrow: { color: colors.muted, fontFamily: fonts.semibold, fontSize: type.caption, letterSpacing: .7 },
  pageTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, letterSpacing: -.8, lineHeight: 38, marginTop: spacing.xxs },
  intro: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23, marginTop: spacing.xs },
  rule: { backgroundColor: colors.line, height: StyleSheet.hairlineWidth, marginVertical: spacing.xl },
  item: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderLeftColor: colors.line, borderLeftWidth: StyleSheet.hairlineWidth, borderRightColor: colors.line, borderRightWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 124, paddingHorizontal: spacing.md, paddingVertical: spacing.md },
  itemFirst: { borderTopColor: colors.line, borderTopLeftRadius: radius.card, borderTopRightRadius: radius.card, borderTopWidth: StyleSheet.hairlineWidth },
  itemLast: { borderBottomColor: colors.line, borderBottomLeftRadius: radius.card, borderBottomRightRadius: radius.card, borderBottomWidth: StyleSheet.hairlineWidth },
  itemDivider: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth },
  itemPressed: { backgroundColor: colors.mint, opacity: 0.84 },
  body: { flex: 1, minWidth: 0 },
  titleRow: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  code: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.label },
  title: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body, lineHeight: 22, marginTop: spacing.xxs },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginTop: spacing.xxs },
  metaText: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xs },
  doneText: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.caption },
  disclaimer: { alignItems: 'flex-start', borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.xs, marginTop: spacing.md, paddingVertical: spacing.md },
  disclaimerText: { color: colors.muted, flex: 1, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
});

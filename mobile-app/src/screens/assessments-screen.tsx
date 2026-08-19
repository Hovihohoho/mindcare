import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { ScreenHeader } from '@/components/screen-header';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { assessmentService, type AssessmentSummary } from '@/services/assessment/assessment.service';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';

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

const filters: FilterOption[] = [
  { id: 'all', label: 'Tất cả bài đánh giá' },
  { id: 'new', label: 'Chưa thực hiện' },
  { id: 'completed', label: 'Đã hoàn thành' },
];

export default function AssessmentsScreen() {
  const router = useRouter();
  const { data, loading, refreshing, error, reload } = useAuthenticatedList(loadAssessments);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const activeFilter = filters.find((item) => item.id === filter);
  const filtered = useMemo(() => data.filter((item) => {
    const textMatch = `${item.title} ${item.description} ${item.code}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
    const filterMatch = filter === 'all' || (filter === 'completed' ? item.completed : !item.completed);
    return textMatch && filterMatch;
  }), [data, filter, query]);

  return (
    <AppScreen>
      <ScreenHeader title="Bài đánh giá" description="Các công cụ sàng lọc giúp bạn quan sát trạng thái hiện tại, không thay thế chẩn đoán y khoa." />
      <ListToolbar activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined} onOpenFilter={() => setSheetOpen(true)} onQueryChange={setQuery} query={query} searchLabel="Tìm bài đánh giá" />
      {loading ? <SkeletonList rows={3} /> : error ? (
        <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Thư viện chưa được tải" />
      ) : filtered.length === 0 ? (
        <DataFeedback actionLabel="Xóa bộ lọc" description="Không có bài đánh giá phù hợp với tìm kiếm hiện tại." kind="empty" onAction={() => { setQuery(''); setFilter('all'); }} title="Chưa có kết quả" />
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
          renderItem={({ item }) => <AssessmentCard item={item} onPress={() => router.push({ pathname: '/assessment/[code]', params: { code: item.code } })} />}
          ListFooterComponent={(
            <View style={styles.disclaimer}>
              <Ionicons color={colors.brandDark} name="shield-checkmark-outline" size={22} />
              <Text style={styles.disclaimerText}>Nguồn, phiên bản và giới hạn sử dụng được hiển thị trước khi bạn bắt đầu.</Text>
            </View>
          )}
        />
      )}
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc bài đánh giá" visible={sheetOpen} />
    </AppScreen>
  );
}

function AssessmentCard({ item, onPress }: { item: AssessmentItem; onPress(): void }) {
  return (
    <Pressable accessibilityLabel={`${item.title}, ${item.questionCount} câu`} accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}>
      <View style={styles.cardTop}>
        <View style={styles.icon}><Ionicons color={colors.brandDark} name="clipboard-outline" size={22} /></View>
        <View style={styles.badge}><Text style={styles.badgeText}>{item.code}</Text></View>
      </View>
      <Text style={styles.title}>{item.title}</Text>
      <Text style={styles.description}>{item.description}</Text>
      <View style={styles.metaRow}>
        <View style={styles.meta}><Ionicons color={colors.muted} name="time-outline" size={16} /><Text style={styles.metaText}>Khoảng 5 phút</Text></View>
        <View style={styles.meta}><Ionicons color={colors.muted} name="list-outline" size={16} /><Text style={styles.metaText}>{item.questionCount} câu</Text></View>
        {item.completed && <View style={styles.done}><Ionicons color={colors.mintInk} name="checkmark-circle" size={16} /><Text style={styles.doneText}>Đã làm</Text></View>}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  list: { gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: 96 },
  card: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, minHeight: 176, padding: spacing.md, ...shadows.card },
  cardPressed: { backgroundColor: colors.surfaceMuted },
  cardTop: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  icon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.input, height: 44, justifyContent: 'center', width: 44 },
  badge: { backgroundColor: colors.mint, borderRadius: radius.pill, paddingHorizontal: spacing.sm, paddingVertical: spacing.xxs },
  badgeText: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.caption },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.cardTitle, lineHeight: 23, marginTop: spacing.sm },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginTop: spacing.xs },
  metaRow: { alignItems: 'center', flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, marginTop: spacing.md },
  meta: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs },
  metaText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  done: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs, marginLeft: 'auto' },
  doneText: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.caption },
  disclaimer: { alignItems: 'flex-start', backgroundColor: colors.surfaceMuted, borderRadius: radius.card, flexDirection: 'row', gap: spacing.sm, marginTop: spacing.xxs, padding: spacing.md },
  disclaimerText: { color: colors.brandDeep, flex: 1, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20 },
});

import Ionicons from '@expo/vector-icons/Ionicons';
import { useMemo, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { ScreenHeader } from '@/components/screen-header';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { expertService, type ExpertSummary } from '@/services/expert/expert.service';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';

async function loadExperts(token: string) {
  const response = await expertService.list(token);
  return response.items;
}

const filters: FilterOption[] = [
  { id: 'all', label: 'Tất cả chuyên môn' },
  { id: 'anxiety', label: 'Lo âu và căng thẳng' },
  { id: 'sleep', label: 'Tâm trạng và giấc ngủ' },
  { id: 'relationship', label: 'Mối quan hệ và gia đình' },
];

export default function ExpertsScreen() {
  const { data, loading, refreshing, error, reload } = useAuthenticatedList(loadExperts);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const activeFilter = filters.find((item) => item.id === filter);
  const filtered = useMemo(() => data.filter((item) => {
    const text = `${item.displayName} ${item.headline ?? ''} ${item.specialties.join(' ')}`.toLocaleLowerCase('vi');
    const matchesQuery = text.includes(query.toLocaleLowerCase('vi'));
    const joined = item.specialties.join(' ').toLocaleLowerCase('vi');
    const matchesFilter = filter === 'all'
      || (filter === 'anxiety' && (joined.includes('lo âu') || joined.includes('căng thẳng')))
      || (filter === 'sleep' && (joined.includes('trầm cảm') || joined.includes('giấc ngủ')))
      || (filter === 'relationship' && (joined.includes('mối quan hệ') || joined.includes('gia đình')));
    return matchesQuery && matchesFilter;
  }), [data, filter, query]);

  return (
    <AppScreen>
      <ScreenHeader title="Chuyên gia" description="Tìm người đồng hành phù hợp với điều bạn đang trải qua và lịch của bạn." />
      <ListToolbar activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined} onOpenFilter={() => setSheetOpen(true)} onQueryChange={setQuery} query={query} searchLabel="Tìm tên hoặc chuyên môn" />
      {loading ? <SkeletonList /> : error ? (
        <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Danh sách chưa được tải" />
      ) : filtered.length === 0 ? (
        <DataFeedback actionLabel="Xóa bộ lọc" description="Chưa có chuyên gia phù hợp với từ khóa và bộ lọc hiện tại." kind="empty" onAction={() => { setQuery(''); setFilter('all'); }} title="Chưa tìm thấy người phù hợp" />
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.expertUserId}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
          renderItem={({ item }) => <ExpertCard item={item} />}
        />
      )}
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc theo chuyên môn" visible={sheetOpen} />
    </AppScreen>
  );
}

function ExpertCard({ item }: { item: ExpertSummary }) {
  const initials = item.displayName.split(/\s+/).slice(-2).map((part) => part[0]).join('').toLocaleUpperCase('vi');
  return (
    <View accessibilityLabel={`Hồ sơ ${item.displayName}`} style={styles.card}>
      <View style={styles.avatar}><Text style={styles.initials}>{initials}</Text></View>
      <View style={styles.body}>
        <Text numberOfLines={1} style={styles.name}>{item.displayName}</Text>
        <Text numberOfLines={1} style={styles.role}>{item.headline || 'Chuyên gia tâm lý MindCare'}</Text>
        <View style={styles.tags}>{item.specialties.slice(0, 2).map((tag) => <View key={tag} style={styles.tag}><Text style={styles.tagText}>{tag}</Text></View>)}</View>
        <View style={styles.metaRow}>
          <View style={styles.meta}><Ionicons color={colors.muted} name="ribbon-outline" size={15} /><Text style={styles.metaText}>{item.yearsOfExperience} năm</Text></View>
          <View style={styles.available}><View style={styles.availableDot} /><Text style={styles.availableText}>{item.consultationCount} lượt tư vấn</Text></View>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  list: { gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: 96 },
  card: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 148, padding: spacing.md, ...shadows.card },
  avatar: { alignItems: 'center', backgroundColor: colors.brandSoft, borderRadius: radius.card, height: 56, justifyContent: 'center', width: 56 },
  initials: { color: colors.brandDeep, fontFamily: fonts.bold, fontSize: type.cardTitle },
  body: { flex: 1, minWidth: 0 },
  name: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.cardTitle },
  role: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xxs },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xxs, marginTop: spacing.xs },
  tag: { backgroundColor: colors.surfaceMuted, borderRadius: radius.pill, paddingHorizontal: spacing.xs, paddingVertical: 3 },
  tagText: { color: colors.brandDeep, fontFamily: fonts.medium, fontSize: 11 },
  metaRow: { alignItems: 'center', flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, marginTop: spacing.sm },
  meta: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs },
  metaText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  available: { alignItems: 'center', flexDirection: 'row', gap: spacing.xxs },
  availableDot: { backgroundColor: colors.mintInk, borderRadius: radius.pill, height: 7, width: 7 },
  availableText: { color: colors.mintInk, fontFamily: fonts.medium, fontSize: 11 },
});

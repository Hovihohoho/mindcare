/* Hallmark · page section: emotion calendar + day history · genre: modern-minimal · theme: MindCare
 * states: loading · data · empty · error · selected day · filtering
 * contrast: pass · mobile: 360–430 dp · pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useMemo, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { DataFeedback } from '@/components/data-states';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { EmotionCalendar, EmotionCalendarSkeleton } from '@/features/emotion/emotion-calendar';
import { emotionOptionMap } from '@/features/emotion/emotion.constants';
import type { EmotionJournal } from '@/features/emotion/emotion.types';
import { useEmotionCalendar } from '@/features/emotion/use-emotion-calendar';
import { JournalCard } from '@/screens/journal-screen';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

const filters: FilterOption[] = [
  { id: 'all', label: 'Tất cả cảm xúc' },
  { id: 'positive', label: 'Tích cực' },
  { id: 'needs-care', label: 'Cần quan tâm' },
];

export default function JournalHistoryScreen() {
  const router = useRouter();
  const listRef = useRef<FlatList<EmotionJournal>>(null);
  const daySectionOffset = useRef(0);
  const today = useMemo(() => startOfDay(new Date()), []);
  const [month, setMonth] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1));
  const [selectedDate, setSelectedDate] = useState(today);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const {
    calendarError, calendarLoading, dayError, dayLoading, entries, points,
    refresh, refreshing, retryCalendar, retryDay,
  } = useEmotionCalendar(month, selectedDate);
  const activeFilter = filters.find((item) => item.id === filter);

  const filtered = useMemo(() => entries.filter((item) => {
    const option = emotionOptionMap[item.emotionType];
    const queryMatch = `${option.label} ${item.content ?? ''}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
    const filterMatch = filter === 'all'
      || (filter === 'positive' ? ['VERY_HAPPY', 'HAPPY'].includes(item.emotionType) : ['SAD', 'STRESSED'].includes(item.emotionType));
    return queryMatch && filterMatch;
  }), [entries, filter, query]);

  const changeMonth = (offset: number) => {
    const next = new Date(month.getFullYear(), month.getMonth() + offset, 1);
    const current = new Date(today.getFullYear(), today.getMonth(), 1);
    if (next > current) return;
    setMonth(next);
    setSelectedDate(next.getFullYear() === current.getFullYear() && next.getMonth() === current.getMonth() ? today : next);
  };

  const selectedLabel = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }).format(selectedDate);
  const filtering = Boolean(query || filter !== 'all');
  const selectDate = (date: Date) => {
    setSelectedDate(date);
    requestAnimationFrame(() => listRef.current?.scrollToOffset({ animated: true, offset: Math.max(0, daySectionOffset.current - spacing.xs) }));
  };

  return (
    <AppScreen>
      <View style={styles.header}>
        <Pressable accessibilityLabel="Quay lại Nhật ký cảm xúc" accessibilityRole="button" onPress={() => router.back()} style={({ pressed }) => [styles.back, pressed && styles.pressed]}>
          <Ionicons color={colors.ink} name="arrow-back" size={22} />
        </Pressable>
        <View style={styles.headerCopy}>
          <Text accessibilityRole="header" style={styles.title}>Lịch sử cảm xúc</Text>
          <Text style={styles.description}>Theo dõi hành trình cảm xúc của bạn qua từng ngày.</Text>
        </View>
      </View>
      <ListToolbar activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined} onOpenFilter={() => setSheetOpen(true)} onQueryChange={setQuery} query={query} searchLabel="Tìm trong ngày đã chọn" />
      <FlatList
        ref={listRef}
        contentContainerStyle={styles.list}
        data={dayLoading || dayError ? [] : filtered}
        keyExtractor={(item) => item.id}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void refresh()} refreshing={refreshing} tintColor={colors.brand} />}
        renderItem={({ item }) => <JournalCard item={item} />}
        ListHeaderComponent={(
          <View style={styles.listHeader}>
            {calendarLoading ? <EmotionCalendarSkeleton /> : calendarError ? (
              <InlineError message={calendarError} onRetry={() => void retryCalendar()} />
            ) : (
              <EmotionCalendar
                canGoNext={month < new Date(today.getFullYear(), today.getMonth(), 1)}
                month={month}
                onChangeMonth={changeMonth}
                onSelectDate={selectDate}
                points={points}
                selectedDate={selectedDate}
              />
            )}
            <View onLayout={(event) => { daySectionOffset.current = event.nativeEvent.layout.y; }} style={styles.dayHeading}>
              <View style={styles.dayHeadingCopy}>
                <Text style={styles.dayTitle}>Chi tiết ngày đã chọn</Text>
                <Text style={styles.dayLabel}>{capitalize(selectedLabel)}</Text>
              </View>
              {!dayLoading && !dayError ? <View style={styles.countBadge}><Text style={styles.countText}>{entries.length}</Text></View> : null}
            </View>
            {dayLoading ? <DayHistorySkeleton /> : null}
            {dayError ? <InlineError message={dayError} onRetry={() => void retryDay()} /> : null}
          </View>
        )}
        ListEmptyComponent={dayLoading || dayError ? null : (
          <DataFeedback
            actionLabel={filtering ? 'Xóa bộ lọc' : 'Chọn ngày khác'}
            description={filtering ? 'Không có bản ghi phù hợp với tìm kiếm và bộ lọc hiện tại.' : 'Ngày này chưa có nhật ký cảm xúc. Hãy chọn một ngày có biểu tượng trên lịch.'}
            kind="empty"
            onAction={() => {
              if (filtering) {
                setQuery('');
                setFilter('all');
              } else {
                setSelectedDate(today);
                setMonth(new Date(today.getFullYear(), today.getMonth(), 1));
              }
            }}
            title={filtering ? 'Không tìm thấy nhật ký' : 'Chưa có nhật ký trong ngày này'}
          />
        )}
      />
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc theo cảm xúc" visible={sheetOpen} />
    </AppScreen>
  );
}

function InlineError({ message, onRetry }: { message: string; onRetry(): void }) {
  return (
    <View style={styles.errorBox}>
      <Ionicons color={colors.danger} name="cloud-offline-outline" size={22} />
      <Text style={styles.errorText}>{message}</Text>
      <Pressable accessibilityRole="button" onPress={onRetry} style={({ pressed }) => [styles.retry, pressed && styles.pressed]}><Text style={styles.retryText}>Thử lại</Text></Pressable>
    </View>
  );
}

function DayHistorySkeleton() {
  return (
    <View accessibilityLabel="Đang tải nhật ký của ngày đã chọn" style={styles.skeletonList}>
      {[0, 1].map((item) => <View key={item} style={styles.skeletonCard}><View style={styles.skeletonIcon} /><View style={styles.skeletonBody}><View style={styles.skeletonTitle} /><View style={styles.skeletonLine} /></View></View>)}
    </View>
  );
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function capitalize(value: string) {
  return value.charAt(0).toLocaleUpperCase('vi') + value.slice(1);
}

const styles = StyleSheet.create({
  header: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: spacing.sm, paddingTop: spacing.lg },
  back: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, height: 44, justifyContent: 'center', width: 44 },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.86 },
  headerCopy: { flex: 1, minWidth: 0 },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.section, lineHeight: 28 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
  list: { gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: 96 },
  listHeader: { gap: spacing.sm },
  dayHeading: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
  dayHeadingCopy: { flex: 1, minWidth: 0 },
  dayTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.cardTitle },
  dayLabel: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xxs },
  countBadge: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.pill, height: 32, justifyContent: 'center', minWidth: 32, paddingHorizontal: spacing.xs },
  countText: { color: colors.brandDark, fontFamily: fonts.bold, fontSize: type.caption },
  errorBox: { alignItems: 'center', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, minHeight: 72, padding: spacing.sm },
  errorText: { color: colors.danger, flex: 1, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  retry: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', paddingHorizontal: spacing.xs },
  retryText: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.caption },
  skeletonList: { gap: spacing.sm },
  skeletonCard: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 104, padding: spacing.md },
  skeletonIcon: { backgroundColor: colors.skeleton, borderRadius: radius.input, height: 48, width: 48 },
  skeletonBody: { flex: 1, gap: spacing.sm },
  skeletonTitle: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 16, width: '52%' },
  skeletonLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '88%' },
});

/* Hallmark · macrostructure: Long Document · tone: soft · anchor hue: MindCare blue
 * pre-emit critique: P5 H5 E5 S5 R5 V5 · designed-as-app
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useMemo, useState, type ReactNode } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';

import { ActionButton } from '@/components/buttons';
import { AppScreen } from '@/components/app-screen';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { useAuth } from '@/features/auth/auth-context';
import { emotionOptionMap } from '@/features/emotion/emotion.constants';
import { EmotionFaceIcon } from '@/features/emotion/emotion-face';
import { EmotionPicker } from '@/features/emotion/emotion-picker';
import { EmotionTrendChart, EmotionTrendSkeleton } from '@/features/emotion/emotion-trend-chart';
import type { EmotionJournal, EmotionLevel } from '@/features/emotion/emotion.types';
import { useEmotionJournal } from '@/features/emotion/use-emotion-journal';
import { emotionService } from '@/services/emotion/emotion.service';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';

const filters: FilterOption[] = [
  { id: 'all', label: 'Tất cả cảm xúc' },
  { id: 'positive', label: 'Tích cực' },
  { id: 'needs-care', label: 'Cần quan tâm' },
];

export default function JournalScreen() {
  const router = useRouter();
  const { session } = useAuth();
  const { entries, trends, loading, refreshing, error, reload, token } = useEmotionJournal();
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const [emotion, setEmotion] = useState<EmotionLevel>();
  const [note, setNote] = useState('');
  const [noteFocused, setNoteFocused] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saved, setSaved] = useState(false);
  const activeFilter = filters.find((item) => item.id === filter);
  const fullName = session?.user.fullName.trim();
  const greetingName = fullName ? fullName.split(/\s+/).slice(-1)[0] : 'bạn';

  const filtered = useMemo(() => entries.filter((item) => {
    const option = emotionOptionMap[item.emotionType];
    const matchesQuery = `${option.label} ${item.content ?? ''}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
    const matchesFilter = filter === 'all'
      || (filter === 'positive' ? ['VERY_HAPPY', 'HAPPY'].includes(item.emotionType) : ['SAD', 'STRESSED'].includes(item.emotionType));
    return matchesQuery && matchesFilter;
  }), [entries, filter, query]);

  const save = async () => {
    if (!token || !emotion || saving) return;
    setSaving(true);
    setSaveError('');
    setSaved(false);
    try {
      await emotionService.create(token, { emotionType: emotion, content: note.trim() });
      setEmotion(undefined);
      setNote('');
      setSaved(true);
      await reload(true);
    } catch (caught) {
      setSaveError(caught instanceof Error ? caught.message : 'Không thể lưu nhật ký. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppScreen>
      <FlatList
        contentContainerStyle={styles.list}
        data={loading || error ? [] : filtered.slice(0, 5)}
        keyExtractor={(item) => item.id}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
        renderItem={({ item }) => <JournalCard item={item} />}
        ListHeaderComponent={(
          <View>
            <View style={styles.hero}>
              <Text style={styles.greeting}>Xin chào {greetingName},</Text>
              <Text accessibilityRole="header" style={styles.heroTitle}>
                Chăm sóc sức khỏe{'\n'}<Text style={styles.heroAccent}>tinh thần</Text> mỗi ngày.
              </Text>
            </View>

            <SectionCard>
              <SectionHeading action="Xem tất cả" onAction={() => router.push('/(tabs)/journal-history')} title="Cảm xúc trong tuần" />
              <Text style={styles.sectionDescription}>Nhìn lại nhịp cảm xúc của bạn trong 7 ngày gần nhất.</Text>
              {loading ? <EmotionTrendSkeleton /> : error ? <InlineError message={error} onRetry={() => void reload()} /> : <EmotionTrendChart points={trends} />}
            </SectionCard>

            <SectionCard>
              <Text style={styles.sectionTitle}>Ghi nhật ký cảm xúc</Text>
              <Text style={styles.sectionDescription}>Hôm nay tâm trạng của bạn như thế nào?</Text>
              <View style={styles.pickerWrap}>
                <EmotionPicker disabled={saving} onChange={(value) => { setEmotion(value); setSaved(false); }} value={emotion} />
              </View>
              <Text style={styles.inputLabel}>Viết thêm về ngày hôm nay</Text>
              <TextInput
                accessibilityLabel="Viết thêm về ngày hôm nay"
                maxLength={5000}
                multiline
                onBlur={() => setNoteFocused(false)}
                onChangeText={(value) => { setNote(value); setSaved(false); }}
                onFocus={() => setNoteFocused(true)}
                placeholder="Hãy chia sẻ những gì đang diễn ra…"
                placeholderTextColor={colors.muted}
                style={[styles.noteInput, noteFocused && styles.noteInputFocused]}
                textAlignVertical="top"
                value={note}
              />
              <View style={styles.formMeta}>
                <Text style={styles.counter}>{note.length}/5000</Text>
                {saved ? <Text accessibilityLiveRegion="polite" style={styles.savedText}>Đã lưu nhật ký</Text> : null}
              </View>
              {saveError ? <InlineError compact message={saveError} onRetry={() => void save()} /> : null}
              <View style={styles.formActions}>
                <ActionButton disabled={saving || (!emotion && !note)} label="Xóa" onPress={() => { setEmotion(undefined); setNote(''); setSaveError(''); }} style={styles.formButton} tone="secondary" />
                <ActionButton disabled={!emotion || saving} label={saveError ? 'Thử lại' : 'Lưu nhật ký'} loading={saving} onPress={() => void save()} style={styles.formButton} />
              </View>
            </SectionCard>

            <View style={styles.historyHeader}>
              <Text style={styles.sectionTitle}>Lịch sử gần đây</Text>
              <Text style={styles.sectionDescription}>Những cảm xúc bạn đã ghi lại gần nhất.</Text>
            </View>
            <View style={styles.toolbarInset}>
              <ListToolbar
                activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined}
                onOpenFilter={() => setSheetOpen(true)}
                onQueryChange={setQuery}
                query={query}
                searchLabel="Tìm trong lịch sử"
              />
            </View>
            {loading ? <HistorySkeleton /> : null}
          </View>
        )}
        ListEmptyComponent={loading ? null : error ? null : (
          <EmptyHistory filtering={Boolean(query || filter !== 'all')} onClear={() => { setQuery(''); setFilter('all'); }} />
        )}
      />
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc theo cảm xúc" visible={sheetOpen} />
    </AppScreen>
  );
}

function SectionCard({ children }: { children: ReactNode }) {
  return <View style={styles.sectionCard}>{children}</View>;
}

function SectionHeading({ title, action, onAction }: { title: string; action: string; onAction(): void }) {
  return (
    <View style={styles.sectionHeading}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <Pressable accessibilityRole="button" onPress={onAction} style={({ pressed }) => [styles.textButton, pressed && styles.pressed]}>
        <Text numberOfLines={1} style={styles.textButtonLabel}>{action}</Text>
        <Ionicons color={colors.brandDark} name="chevron-forward" size={16} />
      </Pressable>
    </View>
  );
}

export function JournalCard({ item }: { item: EmotionJournal }) {
  const option = emotionOptionMap[item.emotionType];
  return (
    <View style={styles.card}>
      <View style={[styles.toneIcon, { backgroundColor: option.surface }]}>
        <EmotionFaceIcon color={option.color} face={option.face} size={34} />
      </View>
      <View style={styles.cardBody}>
        <View style={styles.cardTitleRow}>
          <Text numberOfLines={1} style={styles.cardTitle}>{option.label}</Text>
          <Text style={styles.date}>{formatDate(item.createdAt)}</Text>
        </View>
        <Text numberOfLines={3} style={styles.note}>{item.content || 'Không có ghi chú'}</Text>
      </View>
    </View>
  );
}

function InlineError({ message, onRetry, compact = false }: { message: string; onRetry(): void; compact?: boolean }) {
  return (
    <View style={[styles.inlineError, compact && styles.inlineErrorCompact]}>
      <Ionicons color={colors.danger} name="cloud-offline-outline" size={22} />
      <View style={styles.inlineErrorBody}>
        <Text style={styles.inlineErrorTitle}>Không thể tải dữ liệu</Text>
        <Text style={styles.inlineErrorText}>{message}</Text>
      </View>
      <Pressable accessibilityRole="button" onPress={onRetry} style={({ pressed }) => [styles.retry, pressed && styles.pressed]}>
        <Text numberOfLines={1} style={styles.retryText}>Thử lại</Text>
      </Pressable>
    </View>
  );
}

function EmptyHistory({ filtering, onClear }: { filtering: boolean; onClear(): void }) {
  return (
    <View style={styles.emptyHistory}>
      <View style={styles.emptyIcon}><Ionicons color={colors.brand} name="book-outline" size={24} /></View>
      <Text style={styles.emptyTitle}>{filtering ? 'Không tìm thấy nhật ký' : 'Chưa có nhật ký'}</Text>
      <Text style={styles.emptyText}>{filtering ? 'Thử từ khóa khác hoặc xóa bộ lọc đang áp dụng.' : 'Chọn một cảm xúc phía trên để tạo bản ghi đầu tiên.'}</Text>
      {filtering ? <ActionButton label="Xóa bộ lọc" onPress={onClear} tone="secondary" /> : null}
    </View>
  );
}

function HistorySkeleton() {
  return (
    <View style={styles.skeletonList}>
      {[0, 1, 2].map((item) => (
        <View key={item} style={styles.skeletonCard}>
          <View style={styles.skeletonIcon} />
          <View style={styles.skeletonBody}><View style={styles.skeletonTitle} /><View style={styles.skeletonLine} /></View>
        </View>
      ))}
    </View>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}

const styles = StyleSheet.create({
  list: { gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: 96 },
  hero: { paddingBottom: spacing.xl, paddingTop: spacing.lg },
  greeting: { color: colors.muted, fontFamily: fonts.medium, fontSize: type.body, lineHeight: 22, marginBottom: spacing.xs },
  heroTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, letterSpacing: -0.5, lineHeight: 36 },
  heroAccent: { color: colors.brandDark },
  sectionCard: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginBottom: spacing.md, padding: spacing.md, ...shadows.card },
  sectionHeading: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, justifyContent: 'space-between' },
  sectionTitle: { color: colors.ink, flex: 1, fontFamily: fonts.bold, fontSize: type.section, lineHeight: 28 },
  sectionDescription: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginBottom: spacing.md, marginTop: spacing.xxs },
  textButton: { alignItems: 'center', borderRadius: radius.sm, flexDirection: 'row', height: 44, paddingHorizontal: spacing.xs },
  textButtonLabel: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.caption },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.84 },
  pickerWrap: { marginBottom: spacing.lg },
  inputLabel: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.label, marginBottom: spacing.xs },
  noteInput: { backgroundColor: colors.surface, borderColor: colors.lineStrong, borderRadius: radius.input, borderWidth: 1, color: colors.ink, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, minHeight: 116, padding: spacing.sm },
  noteInputFocused: { borderColor: colors.brand },
  formMeta: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', minHeight: 28 },
  counter: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  savedText: { color: colors.mintInk, fontFamily: fonts.medium, fontSize: type.caption },
  formActions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.xs },
  formButton: { flex: 1 },
  historyHeader: { paddingTop: spacing.xs },
  toolbarInset: { marginHorizontal: -spacing.md },
  card: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 104, padding: spacing.md, ...shadows.card },
  toneIcon: { alignItems: 'center', borderRadius: radius.input, height: 48, justifyContent: 'center', width: 48 },
  cardBody: { flex: 1, minWidth: 0 },
  cardTitleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs, justifyContent: 'space-between' },
  cardTitle: { color: colors.ink, flex: 1, fontFamily: fonts.bold, fontSize: type.cardTitle },
  date: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  note: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginTop: spacing.xs },
  inlineError: { alignItems: 'center', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, minHeight: 88, padding: spacing.sm },
  inlineErrorCompact: { marginTop: spacing.sm },
  inlineErrorBody: { flex: 1, minWidth: 0 },
  inlineErrorTitle: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.label },
  inlineErrorText: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
  retry: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', paddingHorizontal: spacing.xs },
  retryText: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.caption },
  emptyHistory: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, padding: spacing.lg },
  emptyIcon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.input, height: 48, justifyContent: 'center', marginBottom: spacing.sm, width: 48 },
  emptyTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  emptyText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginBottom: spacing.md, marginTop: spacing.xs, textAlign: 'center' },
  skeletonList: { gap: spacing.sm },
  skeletonCard: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 104, padding: spacing.md },
  skeletonIcon: { backgroundColor: colors.skeleton, borderRadius: radius.input, height: 48, width: 48 },
  skeletonBody: { flex: 1, gap: spacing.sm },
  skeletonTitle: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 16, width: '52%' },
  skeletonLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '88%' },
});

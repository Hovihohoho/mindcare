/* Hallmark · macrostructure: Long Document · tone: soft · anchor hue: MindCare blue
 * pre-emit critique: P5 H5 E5 S5 R5 V5 · design-system: design.md · designed-as-app
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, TextInput, View } from 'react-native';

import { ActionButton } from '@/components/buttons';
import { AppScreen } from '@/components/app-screen';
import { SectionHeader } from '@/components/section-header';
import { useAuth } from '@/features/auth/auth-context';
import { emotionOptionMap } from '@/features/emotion/emotion.constants';
import { EmotionFaceIcon } from '@/features/emotion/emotion-face';
import { EmotionPicker } from '@/features/emotion/emotion-picker';
import { EmotionTrendChart, EmotionTrendSkeleton } from '@/features/emotion/emotion-trend-chart';
import type { EmotionJournal, EmotionLevel } from '@/features/emotion/emotion.types';
import { useEmotionJournal } from '@/features/emotion/use-emotion-journal';
import { emotionService } from '@/services/emotion/emotion.service';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

export default function JournalScreen() {
  const router = useRouter();
  const { session } = useAuth();
  const { entries, trends, loading, refreshing, error, reload, token } = useEmotionJournal();
  const [emotion, setEmotion] = useState<EmotionLevel>();
  const [note, setNote] = useState('');
  const [noteFocused, setNoteFocused] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saved, setSaved] = useState(false);
  const fullName = session?.user.fullName.trim();
  const greetingName = fullName ? fullName.split(/\s+/).slice(-1)[0] : 'bạn';

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
        data={loading || error ? [] : entries.slice(0, 5)}
        keyExtractor={(item) => item.id}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
        renderItem={({ item }) => <JournalCard item={item} />}
        ListHeaderComponent={(
          <View>
            <View style={styles.header}>
              <Text accessibilityRole="header" style={styles.greeting}>Xin chào, {greetingName}</Text>
              <Text style={styles.prompt}>Hôm nay bạn cảm thấy thế nào?</Text>
            </View>

            <View style={styles.checkInSection}>
              <View style={styles.pickerWrap}>
                <EmotionPicker disabled={saving} onChange={(value) => { setEmotion(value); setSaved(false); }} value={emotion} />
              </View>
              <Text style={styles.inputLabel}>Điều gì đang ở trong tâm trí bạn?</Text>
              <TextInput
                accessibilityLabel="Viết thêm về ngày hôm nay"
                maxLength={5000}
                multiline
                onBlur={() => setNoteFocused(false)}
                onChangeText={(value) => { setNote(value); setSaved(false); }}
                onFocus={() => setNoteFocused(true)}
                placeholder="Viết một vài dòng nếu bạn muốn…"
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
            </View>

            <View style={styles.section}>
              <SectionHeader actionLabel="Xem chi tiết" onAction={() => router.push('/(tabs)/journal-history')} title="Cảm xúc tuần này" />
              <View style={styles.chartWrap}>
                {loading ? <EmotionTrendSkeleton /> : error ? <InlineError message={error} onRetry={() => void reload()} /> : <EmotionTrendChart points={trends} />}
              </View>
            </View>

            <View style={styles.historyHeader}>
              <SectionHeader actionLabel="Xem tất cả" onAction={() => router.push('/(tabs)/journal-history')} title="Gần đây" />
            </View>
            {loading ? <HistorySkeleton /> : null}
          </View>
        )}
        ListEmptyComponent={loading ? null : error ? null : (
          <EmptyHistory />
        )}
      />
    </AppScreen>
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

function EmptyHistory() {
  return (
    <View style={styles.emptyHistory}>
      <View style={styles.emptyIcon}><Ionicons color={colors.brand} name="book-outline" size={24} /></View>
      <Text style={styles.emptyTitle}>Chưa có nhật ký</Text>
      <Text style={styles.emptyText}>Chọn một cảm xúc phía trên để tạo bản ghi đầu tiên.</Text>
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
  list: { paddingHorizontal: spacing.page, paddingBottom: 96 },
  header: { paddingBottom: spacing.lg, paddingTop: spacing.md },
  greeting: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.title, letterSpacing: -0.7, lineHeight: 38 },
  prompt: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, marginTop: spacing.xxs },
  checkInSection: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, paddingBottom: spacing.xl },
  section: { paddingBottom: spacing.xl, paddingTop: spacing.xl },
  chartWrap: { marginTop: spacing.md },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.84 },
  pickerWrap: { marginBottom: spacing.lg, marginHorizontal: -spacing.xs },
  inputLabel: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.label, marginBottom: spacing.xs },
  noteInput: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, color: colors.ink, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, minHeight: 104, padding: spacing.sm },
  noteInputFocused: { borderColor: colors.brand },
  formMeta: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', minHeight: 28 },
  counter: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  savedText: { color: colors.mintInk, fontFamily: fonts.medium, fontSize: type.caption },
  formActions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.xs },
  formButton: { flex: 1 },
  historyHeader: { borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, paddingTop: spacing.xl },
  card: { alignItems: 'center', borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 88, paddingVertical: spacing.md },
  toneIcon: { alignItems: 'center', height: 44, justifyContent: 'center', width: 44 },
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
  emptyHistory: { alignItems: 'center', padding: spacing.lg },
  emptyIcon: { alignItems: 'center', height: 44, justifyContent: 'center', marginBottom: spacing.xs, width: 44 },
  emptyTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  emptyText: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginBottom: spacing.md, marginTop: spacing.xs, textAlign: 'center' },
  skeletonList: {},
  skeletonCard: { alignItems: 'center', borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 88, paddingVertical: spacing.md },
  skeletonIcon: { backgroundColor: colors.skeleton, borderRadius: radius.pill, height: 40, width: 40 },
  skeletonBody: { flex: 1, gap: spacing.sm },
  skeletonTitle: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 16, width: '52%' },
  skeletonLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '88%' },
});

import Ionicons from '@expo/vector-icons/Ionicons';
import { useMemo, useRef, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { DataFeedback } from '@/components/data-states';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import { aiService, type ChatSource } from '@/services/ai/ai.service';
import { ApiClientError } from '@/services/api/api.client';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type ChatMessage = { id: string; role: 'assistant' | 'user'; text: string; createdAt: string; sources?: ChatSource[] };

const filters: FilterOption[] = [
  { id: 'all', label: 'Toàn bộ cuộc trò chuyện' },
  { id: 'mine', label: 'Tin nhắn của tôi' },
  { id: 'assistant', label: 'Phản hồi từ AI' },
];

const welcome: ChatMessage = {
  id: 'welcome', role: 'assistant',
  text: 'Chào bạn, hôm nay bạn muốn cùng mình gỡ rối điều gì?',
  createdAt: formatTime(new Date()),
};

export default function AiSupportScreen() {
  const { session, logout } = useAuth();
  const listRef = useRef<FlatList<ChatMessage>>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([welcome]);
  const [query, setQuery] = useState('');
  const [draft, setDraft] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState('');
  const [lastQuestion, setLastQuestion] = useState('');
  const activeFilter = filters.find((item) => item.id === filter);
  const filtered = useMemo(() => messages.filter((item) => {
    const queryMatch = item.text.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
    const filterMatch = filter === 'all' || (filter === 'mine' ? item.role === 'user' : item.role === 'assistant');
    return queryMatch && filterMatch;
  }), [filter, messages, query]);

  const send = async (retryQuestion?: string) => {
    const question = (retryQuestion ?? draft).trim();
    if (!question || !session?.accessToken || sending) return;
    if (!retryQuestion) {
      setMessages((current) => [...current, { id: makeId('user'), role: 'user', text: question, createdAt: formatTime(new Date()) }]);
      setDraft('');
    }
    setLastQuestion(question);
    setSending(true);
    setSendError('');
    try {
      const response = await aiService.ask(session.accessToken, question);
      setMessages((current) => [...current, { id: makeId('assistant'), role: 'assistant', text: response.answer, sources: response.sources, createdAt: formatTime(new Date()) }]);
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    } catch (caught) {
      if (caught instanceof ApiClientError && [401, 403].includes(caught.status ?? 0)) void logout().catch(() => undefined);
      setSendError(caught instanceof Error ? caught.message : 'Không thể nhận phản hồi từ AI.');
    } finally {
      setSending(false);
    }
  };

  return (
    <AppScreen>
      <ScreenHeader title="AI hỗ trợ" description="Một không gian riêng tư để sắp xếp suy nghĩ. AI không thay thế chuyên gia hoặc dịch vụ khẩn cấp." />
      <ListToolbar activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined} onOpenFilter={() => setSheetOpen(true)} onQueryChange={setQuery} query={query} searchLabel="Tìm trong cuộc trò chuyện" />
      <FlatList
        ref={listRef}
        contentContainerStyle={styles.list}
        data={filtered}
        keyExtractor={(item) => item.id}
        keyboardDismissMode="interactive"
        keyboardShouldPersistTaps="handled"
        ListEmptyComponent={<DataFeedback actionLabel="Xóa bộ lọc" description="Không có tin nhắn phù hợp với tìm kiếm hiện tại." kind="empty" onAction={() => { setQuery(''); setFilter('all'); }} title="Không tìm thấy tin nhắn" />}
        ListFooterComponent={(
          <View>
            {sending ? <LoadingBubble /> : null}
            {sendError ? (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{sendError}</Text>
                <Pressable accessibilityRole="button" onPress={() => void send(lastQuestion)} style={styles.retry}><Text style={styles.retryText}>Thử lại</Text></Pressable>
              </View>
            ) : null}
          </View>
        )}
        ListHeaderComponent={<Text style={styles.dateLabel}>Hôm nay</Text>}
        renderItem={({ item }) => <MessageBubble item={item} />}
      />
      <View style={styles.composer}>
        <TextInput
          accessibilityLabel="Nhập tin nhắn cho AI"
          editable={!sending}
          maxLength={4000}
          multiline
          onChangeText={setDraft}
          placeholder="Chia sẻ điều bạn đang nghĩ…"
          placeholderTextColor={colors.muted}
          style={styles.composerInput}
          value={draft}
        />
        <Pressable accessibilityLabel="Gửi tin nhắn" accessibilityRole="button" disabled={!draft.trim() || sending} onPress={() => void send()} style={({ pressed }) => [styles.send, pressed && styles.sendPressed, (!draft.trim() || sending) && styles.sendDisabled]}>
          <Ionicons color={colors.surface} name="arrow-up" size={20} />
        </Pressable>
      </View>
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc tin nhắn" visible={sheetOpen} />
    </AppScreen>
  );
}

function MessageBubble({ item }: { item: ChatMessage }) {
  const mine = item.role === 'user';
  return (
    <View style={[styles.messageRow, mine && styles.messageRowMine]}>
      {!mine && <View style={styles.aiIcon}><Ionicons color={colors.brandDark} name="sparkles" size={17} /></View>}
      <View style={[styles.bubble, mine ? styles.mineBubble : styles.aiBubble]}>
        <Text style={[styles.messageText, mine && styles.mineText]}>{item.text}</Text>
        {item.sources?.length ? <Text style={styles.sources}>Nguồn: {item.sources.map((source) => source.title).join(' · ')}</Text> : null}
        <Text style={[styles.messageTime, mine && styles.mineTime]}>{item.createdAt}</Text>
      </View>
    </View>
  );
}

function LoadingBubble() {
  return <View accessibilityLabel="AI đang trả lời" style={styles.messageRow}><View style={styles.aiIcon}><Ionicons color={colors.brandDark} name="sparkles" size={17} /></View><View style={[styles.bubble, styles.aiBubble, styles.loadingBubble]}><View style={styles.loadingLine} /><View style={styles.loadingShortLine} /></View></View>;
}

function formatTime(date: Date) {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(date);
}

function makeId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

const styles = StyleSheet.create({
  list: { flexGrow: 1, gap: spacing.sm, paddingHorizontal: spacing.md, paddingBottom: spacing.md },
  dateLabel: { alignSelf: 'center', color: colors.muted, fontFamily: fonts.medium, fontSize: type.caption, marginBottom: spacing.xxs },
  messageRow: { alignItems: 'flex-end', flexDirection: 'row', gap: spacing.xs, maxWidth: '90%' },
  messageRowMine: { alignSelf: 'flex-end', justifyContent: 'flex-end' },
  aiIcon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.pill, height: 32, justifyContent: 'center', width: 32 },
  bubble: { borderRadius: radius.card, maxWidth: '90%', paddingHorizontal: spacing.sm, paddingVertical: spacing.sm },
  aiBubble: { backgroundColor: colors.surface, borderColor: colors.line, borderBottomLeftRadius: spacing.xxs, borderWidth: 1 },
  mineBubble: { backgroundColor: colors.brandDeep, borderBottomRightRadius: spacing.xxs },
  messageText: { color: colors.ink, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
  mineText: { color: colors.surface },
  sources: { color: colors.brandDark, fontFamily: fonts.medium, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xs },
  messageTime: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xs },
  mineTime: { color: colors.brandSoft, textAlign: 'right' },
  loadingBubble: { gap: spacing.xs, minHeight: 62, width: 180 },
  loadingLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '92%' },
  loadingShortLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '58%' },
  errorBox: { alignItems: 'center', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, marginTop: spacing.sm, padding: spacing.sm },
  errorText: { color: colors.danger, flex: 1, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  retry: { alignItems: 'center', height: 44, justifyContent: 'center', paddingHorizontal: spacing.xs },
  retryText: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.caption },
  composer: { alignItems: 'flex-end', backgroundColor: colors.surface, borderTopColor: colors.line, borderTopWidth: 1, flexDirection: 'row', gap: spacing.xs, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  composerInput: { backgroundColor: colors.canvas, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, maxHeight: 112, minHeight: 48, paddingHorizontal: spacing.sm, paddingVertical: spacing.sm },
  send: { alignItems: 'center', backgroundColor: colors.brandDark, borderRadius: radius.input, height: 48, justifyContent: 'center', width: 48 },
  sendPressed: { opacity: 0.82, transform: [{ translateY: 1 }] },
  sendDisabled: { opacity: 0.42 },
});

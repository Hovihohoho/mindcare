import Ionicons from '@expo/vector-icons/Ionicons';
import { useMemo, useRef, useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ChatBubble, ChatBubbleLoading } from '@/components/chat-bubble';
import { ChatInput } from '@/components/chat-input';
import { DataFeedback } from '@/components/data-states';
import { SearchField } from '@/components/search-field';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import { aiService, type ChatSource } from '@/services/ai/ai.service';
import { ApiClientError } from '@/services/api/api.client';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type ChatMessage = { id: string; role: 'assistant' | 'user'; text: string; createdAt: string; sources?: ChatSource[] };

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
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState('');
  const [lastQuestion, setLastQuestion] = useState('');
  const filtered = useMemo(() => messages.filter((item) => item.text.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'))), [messages, query]);

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
      <ScreenHeader
        action={(
          <Pressable accessibilityLabel="Tùy chọn cuộc trò chuyện" accessibilityRole="button" onPress={() => setMenuOpen((value) => !value)} style={({ pressed }) => [styles.menuButton, pressed && styles.pressed]}>
            <Ionicons color={colors.ink} name="ellipsis-horizontal" size={23} />
          </Pressable>
        )}
        description="Hỗ trợ trò chuyện và sắp xếp suy nghĩ"
        title="AI hỗ trợ"
      />
      {menuOpen ? (
        <View style={styles.menu}>
          <MenuItem icon="time-outline" label="Lịch sử hội thoại" onPress={() => { setMenuOpen(false); Alert.alert('Lịch sử hội thoại', 'Lịch sử sẽ xuất hiện khi tài khoản có cuộc trò chuyện đã lưu.'); }} />
          <MenuItem icon="search-outline" label="Tìm kiếm" onPress={() => { setSearchOpen(true); setMenuOpen(false); }} />
          <MenuItem icon="add-outline" label="Cuộc trò chuyện mới" onPress={() => { setMessages([welcome]); setQuery(''); setSearchOpen(false); setMenuOpen(false); }} />
          <MenuItem icon="information-circle-outline" label="Thông tin & giới hạn của AI" onPress={() => { setMenuOpen(false); Alert.alert('Về AI hỗ trợ', 'AI không thay thế chuyên gia hoặc dịch vụ khẩn cấp. Nếu bạn đang gặp nguy hiểm, hãy liên hệ dịch vụ khẩn cấp tại nơi bạn sống.'); }} />
        </View>
      ) : null}
      {searchOpen ? (
        <View style={styles.searchRow}>
          <View style={styles.searchField}><SearchField label="Tìm trong cuộc trò chuyện" onChangeText={setQuery} value={query} /></View>
          <Pressable accessibilityRole="button" onPress={() => { setSearchOpen(false); setQuery(''); }} style={styles.cancelSearch}><Text style={styles.cancelSearchText}>Đóng</Text></Pressable>
        </View>
      ) : null}
      <FlatList
        ref={listRef}
        contentContainerStyle={styles.list}
        data={filtered}
        keyExtractor={(item) => item.id}
        keyboardDismissMode="interactive"
        keyboardShouldPersistTaps="handled"
        ListEmptyComponent={<DataFeedback actionLabel="Xóa tìm kiếm" description="Không có tin nhắn phù hợp với từ khóa hiện tại." kind="empty" onAction={() => setQuery('')} title="Không tìm thấy tin nhắn" />}
        ListFooterComponent={(
          <View>
            {sending ? <ChatBubbleLoading /> : null}
            {sendError ? (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{sendError}</Text>
                <Pressable accessibilityRole="button" onPress={() => void send(lastQuestion)} style={styles.retry}><Text style={styles.retryText}>Thử lại</Text></Pressable>
              </View>
            ) : null}
          </View>
        )}
        ListHeaderComponent={<Text style={styles.dateLabel}>Hôm nay</Text>}
        renderItem={({ item }) => <ChatBubble role={item.role} sources={item.sources?.map((source) => source.title)} text={item.text} time={item.createdAt} />}
      />
      <ChatInput disabled={sending} onChangeText={setDraft} onSend={() => void send()} value={draft} />
    </AppScreen>
  );
}

function MenuItem({ icon, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress(): void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.menuItem, pressed && styles.menuItemPressed]}>
      <Ionicons color={colors.inkSoft} name={icon} size={20} />
      <Text style={styles.menuItemText}>{label}</Text>
    </Pressable>
  );
}

function formatTime(date: Date) {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(date);
}

function makeId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

const styles = StyleSheet.create({
  list: { flexGrow: 1, gap: spacing.md, paddingHorizontal: spacing.page, paddingBottom: spacing.md },
  dateLabel: { alignSelf: 'center', color: colors.muted, fontFamily: fonts.medium, fontSize: type.caption, marginBottom: spacing.xxs },
  menuButton: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', width: 44 },
  pressed: { opacity: 0.65 },
  menu: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginBottom: spacing.sm, marginHorizontal: spacing.page, overflow: 'hidden' },
  menuItem: { alignItems: 'center', borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 48, paddingHorizontal: spacing.sm },
  menuItemPressed: { backgroundColor: colors.surfacePressed },
  menuItemText: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.label },
  searchRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs, marginBottom: spacing.sm, paddingHorizontal: spacing.page },
  searchField: { flex: 1 },
  cancelSearch: { alignItems: 'center', minHeight: 44, justifyContent: 'center' },
  cancelSearchText: { color: colors.brand, fontFamily: fonts.medium, fontSize: type.label },
  errorBox: { alignItems: 'center', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, marginTop: spacing.sm, padding: spacing.sm },
  errorText: { color: colors.danger, flex: 1, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  retry: { alignItems: 'center', height: 44, justifyContent: 'center', paddingHorizontal: spacing.xs },
  retryText: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.caption },
});

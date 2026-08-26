import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, FlatList, Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ChatBubble, ChatBubbleLoading } from '@/components/chat-bubble';
import { ChatInput } from '@/components/chat-input';
import { DataFeedback } from '@/components/data-states';
import { SearchField } from '@/components/search-field';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import { aiService, type ChatSafetyDirective, type ChatSource, type ConversationSummary } from '@/services/ai/ai.service';
import { ApiClientError } from '@/services/api/api.client';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type ChatMessage = { id: string; role: 'assistant' | 'user'; text: string; createdAt: string; sources?: ChatSource[]; safety?: ChatSafetyDirective };

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
  const [conversationId, setConversationId] = useState<string>();
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [historyOpen, setHistoryOpen] = useState(false);
  const filtered = useMemo(() => messages.filter((item) => item.text.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'))), [messages, query]);

  const refreshHistory = () => {
    if (!session?.accessToken) return;
    void aiService.listConversations(session.accessToken).then(setConversations).catch(() => undefined);
  };
  useEffect(refreshHistory, [session?.accessToken]);

  const openConversation = async (id: string) => {
    if (!session?.accessToken) return;
    const conversation = await aiService.getConversation(session.accessToken, id);
    setConversationId(id);
    setMessages(conversation.messages.map((message) => ({ id: message.id, role: message.role, text: message.content, sources: message.sources, safety: safetyFromLevel(message.safetyLevel), createdAt: formatTime(new Date(message.createdAt)) })));
    setHistoryOpen(false);
  };

  const startNewConversation = () => {
    setConversationId(undefined);
    setMessages([welcome]);
    setHistoryOpen(false);
    setQuery('');
    setSearchOpen(false);
  };

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
      const response = await aiService.ask(session.accessToken, question, conversationId);
      setMessages((current) => [...current, { id: makeId('assistant'), role: 'assistant', text: response.answer, sources: response.sources, safety: response.safety, createdAt: formatTime(new Date()) }]);
      setConversationId(response.conversationId);
      refreshHistory();
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
          <MenuItem icon="time-outline" label="Lịch sử hội thoại" onPress={() => { setMenuOpen(false); setHistoryOpen(true); refreshHistory(); }} />
          <MenuItem icon="search-outline" label="Tìm kiếm" onPress={() => { setSearchOpen(true); setMenuOpen(false); }} />
          <MenuItem icon="add-outline" label="Cuộc trò chuyện mới" onPress={() => { setMenuOpen(false); startNewConversation(); }} />
          <MenuItem icon="information-circle-outline" label="Thông tin & giới hạn của AI" onPress={() => { setMenuOpen(false); Alert.alert('Về AI hỗ trợ', 'Hội thoại được lưu vào tài khoản và bạn có thể xóa trong lịch sử. AI không thay thế dịch vụ khẩn cấp. Nếu bạn đang gặp nguy hiểm, hãy liên hệ dịch vụ khẩn cấp tại nơi bạn sống.'); }} />
        </View>
      ) : null}
      {historyOpen ? (
        <View style={styles.historyPanel}>
          <View style={styles.historyHeader}><Text style={styles.historyTitle}>Lịch sử hội thoại</Text><Pressable accessibilityRole="button" onPress={() => setHistoryOpen(false)}><Ionicons color={colors.ink} name="close" size={24} /></Pressable></View>
          {conversations.length === 0 ? <Text style={styles.historyEmpty}>Chưa có cuộc trò chuyện đã lưu.</Text> : conversations.map((conversation) => (
            <View key={conversation.id} style={styles.historyRow}>
              <Pressable accessibilityRole="button" onPress={() => void openConversation(conversation.id)} style={styles.historyOpen}><Text numberOfLines={1} style={styles.historyLabel}>{conversation.title}</Text></Pressable>
              <Pressable accessibilityLabel="Xóa cuộc trò chuyện" accessibilityRole="button" onPress={() => Alert.alert('Xóa cuộc trò chuyện?', 'Thao tác này không thể hoàn tác.', [{ text: 'Hủy', style: 'cancel' }, { text: 'Xóa', style: 'destructive', onPress: () => { if (!session?.accessToken) return; void aiService.deleteConversation(session.accessToken, conversation.id).then(() => { if (conversation.id === conversationId) startNewConversation(); refreshHistory(); }); } }])} style={styles.historyDelete}><Ionicons color={colors.danger} name="trash-outline" size={20} /></Pressable>
            </View>
          ))}
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
        renderItem={({ item }) => <View><ChatBubble role={item.role} sources={item.sources?.map((source) => source.title)} text={item.text} time={item.createdAt} />{item.safety?.showSafetyCheck ? <SafetyCard safety={item.safety} /> : null}</View>}
      />
      <ChatInput disabled={sending} onChangeText={setDraft} onSend={() => void send()} value={draft} />
    </AppScreen>
  );
}

function safetyFromLevel(level: ChatSafetyDirective['level']): ChatSafetyDirective {
  const urgent = level === 'EXPLICIT' || level === 'IMMINENT';
  return { level, showSafetyCheck: level !== 'NONE', showEmergencyActions: urgent, emergencyNumber: urgent ? '115' : null };
}

function SafetyCard({ safety }: { safety: ChatSafetyDirective }) {
  const urgent = safety.showEmergencyActions;
  const emergencyNumber = safety.emergencyNumber ?? '115';
  return (
    <View accessibilityRole="alert" style={[styles.safetyCard, urgent ? styles.safetyUrgent : styles.safetyCheck]}>
      <View style={styles.safetyTitleRow}><Ionicons color={urgent ? colors.danger : colors.ink} name="shield-checkmark-outline" size={21} /><Text style={styles.safetyTitle}>{urgent ? 'Ưu tiên an toàn ngay lúc này' : 'Kiểm tra sự an toàn của bạn'}</Text></View>
      <Text style={styles.safetyText}>{urgent ? 'Nếu bạn có thể gây hại cho bản thân ngay lúc này, đừng ở một mình. Hãy gọi hỗ trợ khẩn cấp hoặc đến khoa cấp cứu gần nhất.' : 'Bạn có đang nghĩ đến việc làm hại bản thân hoặc cảm thấy mình không thể giữ an toàn không?'}</Text>
      {urgent ? <Pressable accessibilityRole="button" onPress={() => void Linking.openURL(`tel:${emergencyNumber}`)} style={styles.emergencyButton}><Ionicons color="#fff" name="call" size={18} /><Text style={styles.emergencyButtonText}>Gọi cấp cứu {emergencyNumber}</Text></Pressable> : null}
      {urgent ? <Text style={styles.safetyText}>Hãy nhờ một người đáng tin cậy ở bên và di chuyển xa các vật có thể gây hại nếu bạn làm được an toàn.</Text> : null}
      <Text style={styles.safetyNote}>MindCare không tự động gọi cấp cứu hoặc thông báo cho người khác.</Text>
    </View>
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
  historyPanel: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.xs, marginBottom: spacing.sm, marginHorizontal: spacing.page, maxHeight: 280, overflow: 'hidden', padding: spacing.sm },
  historyHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', minHeight: 44 },
  historyTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.label },
  historyEmpty: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, paddingBottom: spacing.sm },
  historyRow: { alignItems: 'center', borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, flexDirection: 'row', minHeight: 48 },
  historyOpen: { flex: 1, justifyContent: 'center', minHeight: 48 },
  historyLabel: { color: colors.ink, fontFamily: fonts.regular, fontSize: type.caption },
  historyDelete: { alignItems: 'center', height: 44, justifyContent: 'center', width: 44 },
  safetyCard: { borderRadius: radius.card, borderWidth: 1, gap: spacing.sm, marginTop: spacing.xs, padding: spacing.md },
  safetyUrgent: { backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine },
  safetyCheck: { backgroundColor: colors.surface, borderColor: colors.line },
  safetyTitleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs },
  safetyTitle: { color: colors.ink, flex: 1, fontFamily: fonts.semibold, fontSize: type.label },
  safetyText: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 19 },
  safetyNote: { color: colors.muted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 16 },
  emergencyButton: { alignItems: 'center', alignSelf: 'flex-start', backgroundColor: colors.danger, borderRadius: radius.input, flexDirection: 'row', gap: spacing.xs, minHeight: 44, paddingHorizontal: spacing.md },
  emergencyButtonText: { color: '#fff', fontFamily: fonts.semibold, fontSize: type.label },
});

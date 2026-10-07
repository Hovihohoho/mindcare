import { useCallback, useEffect, useRef, useState } from 'react';
import { Redirect, router, useFocusEffect } from 'expo-router';
import { Alert, FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { ScreenHeader } from '@/components/screen-header';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { useAuth } from '@/features/auth/auth-context';
import { useNotifications } from '@/features/notifications/notification-context';
import { notificationDestination } from '@/features/notifications/notification-navigation';
import { notificationService, type NotificationItem, type NotificationPage } from '@/services/notifications/notification.service';
import { colors, fonts, spacing, type } from '@/theme/tokens';

export default function NotificationInboxScreen() {
  const { session, status } = useAuth();
  const token = session?.accessToken;
  const { unreadCount, revision, setCount } = useNotifications();
  const [page, setPage] = useState<NotificationPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const request = useRef(0);
  const mutating = useRef(false);
  const activeToken = useRef(token);
  activeToken.current = token;
  const mounted = useRef(true);
  const focused = useRef(false);
  const pageRef = useRef(page);
  pageRef.current = page;
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  const load = useCallback(async (next = 0) => {
    if (!token || mutating.current) return;
    const current = ++request.current;
    setLoading(true); setError(null);
    try {
      const result = await notificationService.getNotifications(token, next);
      if (current !== request.current || !mounted.current || token !== activeToken.current) return;
      setPage((previous) => ({ ...result, items: next > 0 && previous ? [...previous.items, ...result.items.filter((item) => !previous.items.some((old) => old.id === item.id))] : result.items }));
      setCount(result.unreadCount);
    } catch (cause) {
      if (current === request.current && mounted.current && token === activeToken.current) setError(cause instanceof Error ? cause.message : 'Không thể tải thông báo.');
    } finally { if (current === request.current && mounted.current && token === activeToken.current) setLoading(false); }
  }, [token, setCount]);
  useEffect(() => { request.current++; setPage(null); }, [token]);
  useFocusEffect(useCallback(() => { focused.current = true; void load(); return () => { focused.current = false; }; }, [load]));
  useEffect(() => { if (focused.current) void load(); }, [load, revision]);

  async function mark(item?: NotificationItem) {
    if (!token || mutating.current) return;
    ++request.current;
    mutating.current = true; setBusy(true); setLoading(false); setError(null);
    try {
      if (!item || !item.readAt) {
        if (item) await notificationService.markNotificationRead(token, item.id);
        else await notificationService.markAllNotificationsRead(token);
      }
      if (!mounted.current || token !== activeToken.current) return;
      const readAt = new Date().toISOString();
      const count = item ? Math.max(0, (unreadCount ?? pageRef.current?.unreadCount ?? 0) - (item.readAt ? 0 : 1)) : 0;
      setPage((previous) => previous && ({ ...previous, unreadCount: count, items: previous.items.map((entry) => !item || entry.id === item.id ? { ...entry, readAt: entry.readAt ?? readAt } : entry) }));
      setCount(count);
      if (item && focused.current) {
        const destination = notificationDestination(item.actionUrl);
        if (destination) router.navigate(destination);
        else Alert.alert(item.title, item.message);
      }
    } catch (cause) {
      if (mounted.current && token === activeToken.current) setError(cause instanceof Error ? cause.message : 'Không thể đánh dấu đã đọc.');
    } finally { mutating.current = false; if (mounted.current) setBusy(false); }
  }

  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  return <AppScreen>
    <ScreenHeader title="Thông báo" onBack={() => router.back()} />
    {(unreadCount ?? 0) > 0 && <Pressable accessibilityRole="button" disabled={busy || loading} onPress={() => void mark()} style={styles.action}><Text style={styles.link}>{busy ? 'Đang cập nhật…' : 'Đánh dấu tất cả đã đọc'}</Text></Pressable>}
    {error && <DataFeedback kind="error" title="Không thể cập nhật thông báo" description={error} actionLabel="Thử lại" onAction={() => void load()} />}
    {!page && loading ? <SkeletonList /> : page && <FlatList
      data={page.items} keyExtractor={(item) => item.id} contentContainerStyle={styles.list}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={() => void load()} colors={[colors.brand]} />}
      ListEmptyComponent={!error ? <DataFeedback kind="empty" title="Chưa có thông báo" description="Thông báo mới sẽ xuất hiện tại đây." /> : null}
      ListFooterComponent={page.page + 1 < page.totalPages ? <Pressable disabled={loading || busy} onPress={() => void load(page.page + 1)} style={styles.action}><Text style={styles.link}>Tải thêm</Text></Pressable> : null}
      renderItem={({ item }) => <Pressable accessibilityRole="button" accessibilityLabel={`${item.readAt ? 'Đã đọc' : 'Chưa đọc'}: ${item.title}`} disabled={busy || loading} onPress={() => void mark(item)} style={[styles.row, !item.readAt && styles.unread]}>
        <View style={styles.heading}><Text style={styles.title}>{item.title}</Text>{!item.readAt && <Text style={styles.link}>●</Text>}</View>
        <Text style={styles.message}>{item.message}</Text>
        <Text style={styles.date}>{new Date(item.createdAt).toLocaleString('vi-VN')} · {item.readAt ? 'Đã đọc' : 'Chưa đọc'}</Text>
      </Pressable>}
    />}
  </AppScreen>;
}
const styles = StyleSheet.create({
  list: { paddingHorizontal: spacing.page, paddingBottom: spacing.xl },
  action: { padding: spacing.md, alignItems: 'center' },
  link: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.label },
  row: { padding: spacing.md, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line, backgroundColor: colors.surface },
  unread: { backgroundColor: colors.brandSoft },
  heading: { flexDirection: 'row', gap: spacing.sm },
  title: { flex: 1, color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body },
  message: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, marginTop: spacing.xs },
  date: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xs },
});

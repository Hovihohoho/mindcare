import Ionicons from '@expo/vector-icons/Ionicons';
import { Redirect, router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Alert, FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { ActionButton } from '@/components/buttons';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import type { LoginSession } from '@/features/auth/auth.types';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { currentSessionId, authService } from '@/services/auth/auth.service';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

function dateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Thời gian không hợp lệ' : date.toLocaleString('vi-VN');
}

export default function SessionsScreen() {
  const { session, status } = useAuth();
  const token = session?.accessToken;
  const { data, loading, refreshing, error, reload } = useAuthenticatedList<LoginSession>(authService.sessions);
  const [revokingId, setRevokingId] = useState<string | null>(null);
  const [revokedLocally, setRevokedLocally] = useState<Set<string>>(() => new Set());
  const [actionError, setActionError] = useState('');
  const currentId = useMemo(() => token ? currentSessionId(token) : null, [token]);

  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (status === 'loading' || !session || loading) return <AppScreen><ScreenHeader title="Phiên đăng nhập" onBack={() => router.back()} /><SkeletonList rows={3} /></AppScreen>;

  const revoke = async (item: LoginSession) => {
    if (!token || item.id === currentId || item.revoked || revokedLocally.has(item.id)) return;
    setRevokingId(item.id); setActionError('');
    try {
      await authService.revokeSession(token, item.id);
      setRevokedLocally((previous) => new Set(previous).add(item.id));
      await reload(true);
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Không thể thu hồi phiên đăng nhập.');
    } finally { setRevokingId(null); }
  };

  const confirmRevoke = (item: LoginSession) => Alert.alert(
    'Thu hồi phiên đăng nhập?',
    item.userAgent || 'Phiên này sẽ không thể truy cập tài khoản nữa.',
    [
      { text: 'Hủy', style: 'cancel' },
      { text: 'Thu hồi', style: 'destructive', onPress: () => void revoke(item) },
    ],
  );

  const renderSession = ({ item }: { item: LoginSession }) => {
    const isCurrent = currentId !== null && item.id === currentId;
    const isRevoked = item.revoked || revokedLocally.has(item.id);
    const isExpired = Date.parse(item.expiresAt) <= Date.now();
    return <View style={styles.card}>
      <View style={styles.cardHeading}>
        <Ionicons color={isCurrent ? colors.brand : colors.muted} name={isCurrent ? 'phone-portrait-outline' : 'laptop-outline'} size={21} />
        <Text style={styles.cardTitle}>{isCurrent ? 'Phiên hiện tại' : 'Phiên đăng nhập'}</Text>
        <Text style={[styles.status, isCurrent ? styles.current : (isRevoked || isExpired) ? styles.inactive : styles.active]}>
          {isCurrent ? 'Đang dùng' : isRevoked ? 'Đã thu hồi' : isExpired ? 'Đã hết hạn' : 'Đang hoạt động'}
        </Text>
      </View>
      {item.userAgent ? <Text selectable style={styles.detail}>Ứng dụng / trình duyệt: {item.userAgent}</Text> : null}
      {item.ipAddress ? <Text selectable style={styles.detail}>Địa chỉ IP: {item.ipAddress}</Text> : null}
      <Text style={styles.detail}>Tạo lúc: {dateTime(item.createdAt)}</Text>
      <Text style={styles.detail}>Hoạt động gần nhất: {dateTime(item.lastSeenAt)}</Text>
      <Text style={styles.detail}>Hết hạn: {dateTime(item.expiresAt)}</Text>
      {!isCurrent && !isRevoked && !isExpired && currentId ? (
        <ActionButton compact disabled={revokingId !== null} label={revokingId === item.id ? 'Đang thu hồi' : 'Thu hồi phiên này'} loading={revokingId === item.id} onPress={() => confirmRevoke(item)} tone="danger" />
      ) : null}
    </View>;
  };

  return <AppScreen>
    <ScreenHeader title="Phiên đăng nhập" onBack={() => router.back()} />
    <View style={styles.body}>
      <Text style={styles.intro}>Xem các phiên đã đăng nhập và thu hồi phiên trên thiết bị khác.</Text>
      {currentId === null ? <Text style={styles.note}>Không xác định được phiên hiện tại từ token; thao tác thu hồi được tạm ẩn để tránh đăng xuất nhầm.</Text> : null}
      {error ? <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Không thể tải phiên đăng nhập" /> : null}
      {actionError ? <Text accessibilityRole="alert" style={styles.error}>{actionError}</Text> : null}
      {!error ? <FlatList
        data={data}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
        ListEmptyComponent={<DataFeedback description="Các phiên đăng nhập sẽ xuất hiện tại đây." kind="empty" title="Chưa có phiên đăng nhập" />}
        renderItem={renderSession}
      /> : null}
    </View>
  </AppScreen>;
}

const styles = StyleSheet.create({
  body: { flex: 1, paddingHorizontal: spacing.page },
  intro: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, paddingBottom: spacing.sm, paddingTop: spacing.xs },
  note: { backgroundColor: colors.warningSoft, borderRadius: radius.sm, color: colors.warning, fontFamily: fonts.medium, fontSize: type.caption, lineHeight: 19, marginBottom: spacing.sm, padding: spacing.sm },
  error: { backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.sm, borderWidth: 1, color: colors.danger, fontFamily: fonts.medium, fontSize: type.label, marginBottom: spacing.sm, padding: spacing.sm },
  list: { gap: spacing.sm, paddingBottom: spacing.xxl },
  card: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.xs, padding: spacing.md },
  cardHeading: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs },
  cardTitle: { color: colors.ink, flex: 1, fontFamily: fonts.semibold, fontSize: type.body },
  status: { borderRadius: radius.pill, fontFamily: fonts.semibold, fontSize: type.caption, overflow: 'hidden', paddingHorizontal: spacing.xs, paddingVertical: 3 },
  current: { backgroundColor: colors.brandSoft, color: colors.brandDeep },
  active: { backgroundColor: colors.mint, color: colors.brandDeep },
  inactive: { backgroundColor: colors.surfaceMuted, color: colors.muted },
  detail: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 19 },
});

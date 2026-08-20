import Ionicons from '@expo/vector-icons/Ionicons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { FilterBottomSheet, type FilterOption } from '@/components/filter-controls';
import { ListToolbar } from '@/components/list-toolbar';
import { ScreenHeader } from '@/components/screen-header';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { authService } from '@/services/auth/auth.service';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';
import type { SettingItem } from '@/types/mindcare';
import { useAuth } from '@/features/auth/auth-context';

const iconMap: Record<SettingItem['icon'], keyof typeof Ionicons.glyphMap> = {
  person: 'person-outline', notifications: 'notifications-outline', lock: 'lock-closed-outline', language: 'language-outline', shield: 'shield-checkmark-outline', fitness: 'fitness-outline',
};

const filters: FilterOption[] = [
  { id: 'all', label: 'Tất cả cài đặt' },
  { id: 'account', label: 'Tài khoản' },
  { id: 'privacy', label: 'Quyền riêng tư và bảo mật' },
  { id: 'app', label: 'Ứng dụng' },
];

async function loadSettings(token: string): Promise<SettingItem[]> {
  const [user, sessions] = await Promise.all([authService.me(token), authService.sessions(token)]);
  const activeSessions = sessions.filter((item) => !item.revoked).length;
  return [
    { id: 's6', title: 'Google Health Connect', description: 'Quản lý quyền và đồng bộ dữ liệu sức khỏe', icon: 'fitness' },
    { id: 's1', title: 'Hồ sơ cá nhân', description: `${user.fullName} · ${user.email}`, icon: 'person' },
    { id: 's2', title: 'Xác thực email', description: user.emailVerified ? 'Email đã được xác thực' : 'Email chưa được xác thực', icon: 'notifications' },
    { id: 's3', title: 'Trạng thái tài khoản', description: user.active ? 'Tài khoản đang hoạt động' : 'Tài khoản đã bị vô hiệu hóa', icon: 'lock' },
    { id: 's4', title: 'Ngôn ngữ', description: 'Tiếng Việt', icon: 'language' },
    { id: 's5', title: 'Phiên đăng nhập', description: `${activeSessions} phiên đang hoạt động`, icon: 'shield' },
  ];
}

export default function SettingsScreen() {
  const { logout } = useAuth();
  const { data, loading, refreshing, error, reload } = useAuthenticatedList(loadSettings);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [sheetOpen, setSheetOpen] = useState(false);
  const activeFilter = filters.find((item) => item.id === filter);
  const filtered = useMemo(() => data.filter((item) => {
    const queryMatch = `${item.title} ${item.description}`.toLocaleLowerCase('vi').includes(query.toLocaleLowerCase('vi'));
    const filterMatch = filter === 'all'
      || (filter === 'account' && ['s1', 's2'].includes(item.id))
      || (filter === 'privacy' && ['s3', 's5', 's6'].includes(item.id))
      || (filter === 'app' && item.id === 's4');
    return queryMatch && filterMatch;
  }), [data, filter, query]);

  return (
    <AppScreen>
      <ScreenHeader title="Cài đặt" description="Quản lý tài khoản, quyền riêng tư và cách MindCare đồng hành cùng bạn." />
      <ListToolbar activeFilterLabel={filter !== 'all' ? activeFilter?.label : undefined} onOpenFilter={() => setSheetOpen(true)} onQueryChange={setQuery} query={query} searchLabel="Tìm cài đặt" />
      {loading ? <SkeletonList rows={3} /> : error ? (
        <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Cài đặt chưa được tải" />
      ) : filtered.length === 0 ? (
        <DataFeedback actionLabel="Xóa bộ lọc" description="Không có mục cài đặt phù hợp với tìm kiếm hiện tại." kind="empty" onAction={() => { setQuery(''); setFilter('all'); }} title="Không tìm thấy cài đặt" />
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
          renderItem={({ item }) => <SettingRow item={item} onPress={item.id === 's6' ? () => router.push('/(tabs)/health-connect') : undefined} />}
          ListFooterComponent={(
            <Pressable accessibilityRole="button" onPress={() => void logout().catch(() => undefined)} style={({ pressed }) => [styles.signOut, pressed && styles.pressed]}>
              <Ionicons color={colors.danger} name="log-out-outline" size={20} />
              <Text style={styles.signOutText}>Đăng xuất</Text>
            </Pressable>
          )}
        />
      )}
      <FilterBottomSheet onClose={() => setSheetOpen(false)} onSelect={setFilter} options={filters} selected={filter} title="Lọc nhóm cài đặt" visible={sheetOpen} />
    </AppScreen>
  );
}

function SettingRow({ item, onPress }: { item: SettingItem; onPress?: () => void }) {
  return (
    <Pressable accessibilityRole={onPress ? 'button' : undefined} disabled={!onPress} onPress={onPress} style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <View style={styles.icon}><Ionicons color={colors.brandDark} name={iconMap[item.icon]} size={21} /></View>
      <View style={styles.body}>
        <Text style={styles.title}>{item.title}</Text>
        <Text numberOfLines={1} style={styles.description}>{item.description}</Text>
      </View>
      <Ionicons color={onPress ? colors.brandDark : colors.mintInk} name={onPress ? 'chevron-forward' : 'checkmark-circle-outline'} size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  list: { gap: spacing.xs, paddingHorizontal: spacing.md, paddingBottom: 96 },
  row: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 76, padding: spacing.sm, ...shadows.card },
  pressed: { backgroundColor: colors.surfaceMuted, opacity: 0.86 },
  icon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.input, height: 44, justifyContent: 'center', width: 44 },
  body: { flex: 1, minWidth: 0 },
  title: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xxs },
  signOut: { alignItems: 'center', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, height: 52, justifyContent: 'center', marginTop: spacing.xs },
  signOutText: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.label },
});

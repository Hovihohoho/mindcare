import { router } from 'expo-router';
import { Pressable, RefreshControl, SectionList, StyleSheet, Text } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ListItem } from '@/components/list-item';
import { ScreenHeader } from '@/components/screen-header';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { authService } from '@/services/auth/auth.service';
import { colors, fonts, spacing, type } from '@/theme/tokens';
import type { SettingItem } from '@/types/mindcare';
import { useAuth } from '@/features/auth/auth-context';

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
  const sections = [
    { title: 'Tài khoản', data: data.filter((item) => ['s1', 's2', 's3'].includes(item.id)) },
    { title: 'Sức khỏe & dữ liệu', data: data.filter((item) => item.id === 's6') },
    { title: 'Ứng dụng', data: data.filter((item) => item.id === 's4') },
    { title: 'Bảo mật', data: data.filter((item) => item.id === 's5') },
  ].filter((section) => section.data.length);

  return (
    <AppScreen>
      <ScreenHeader title="Cài đặt" />
      {loading ? <SkeletonList rows={3} /> : error ? (
        <DataFeedback actionLabel="Thử lại" description={error} kind="error" onAction={() => void reload()} title="Cài đặt chưa được tải" />
      ) : data.length === 0 ? (
        <DataFeedback description="Các tùy chọn tài khoản sẽ xuất hiện tại đây." kind="empty" title="Chưa có cài đặt" />
      ) : (
        <SectionList
          sections={sections}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl colors={[colors.brand]} onRefresh={() => void reload(true)} refreshing={refreshing} tintColor={colors.brand} />}
          renderSectionHeader={({ section }) => <Text style={styles.sectionTitle}>{section.title}</Text>}
          renderItem={({ item, index, section }) => (
            <ListItem
              description={item.description}
              icon={iconMap[item.icon]}
              onPress={item.id === 's6' ? () => router.push('/(tabs)/health-connect') : undefined}
              showDivider={index !== section.data.length - 1}
              title={item.title}
            />
          )}
          ListFooterComponent={(
            <Pressable accessibilityRole="button" onPress={() => void logout().catch(() => undefined)} style={({ pressed }) => [styles.signOut, pressed && styles.pressed]}>
              <Text style={styles.signOutText}>Đăng xuất</Text>
            </Pressable>
          )}
        />
      )}
    </AppScreen>
  );
}

const iconMap = {
  person: 'person-outline', notifications: 'mail-outline', lock: 'checkmark-circle-outline', language: 'language-outline', shield: 'shield-checkmark-outline', fitness: 'fitness-outline',
} as const;

const styles = StyleSheet.create({
  list: { paddingHorizontal: spacing.page, paddingBottom: 96 },
  sectionTitle: { color: colors.tertiary, fontFamily: fonts.semibold, fontSize: type.caption, letterSpacing: 0.2, marginTop: spacing.lg, paddingBottom: spacing.xxs },
  pressed: { backgroundColor: colors.surfaceMuted, opacity: 0.86 },
  signOut: { alignItems: 'center', borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, height: 56, justifyContent: 'center', marginTop: spacing.lg },
  signOutText: { color: colors.danger, fontFamily: fonts.medium, fontSize: type.label },
});

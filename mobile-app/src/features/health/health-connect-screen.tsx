import Ionicons from '@expo/vector-icons/Ionicons';
import { router } from 'expo-router';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ActionButton } from '@/components/buttons';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ScreenHeader } from '@/components/screen-header';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';
import type { HealthDataType } from './health.types';
import { useHealthConnect } from './use-health-connect';

const permissionLabels: { key: HealthDataType; icon: keyof typeof Ionicons.glyphMap; label: string }[] = [
  { key: 'steps', icon: 'footsteps-outline', label: 'Bước chân' },
  { key: 'sleep', icon: 'moon-outline', label: 'Giấc ngủ' },
  { key: 'heartRate', icon: 'heart-outline', label: 'Nhịp tim' },
  { key: 'exercise', icon: 'fitness-outline', label: 'Hoạt động thể chất' },
];

const statusCopy = {
  connected: { label: 'Đã kết nối', tone: 'success' as const },
  partial: { label: 'Thiếu một số quyền', tone: 'warning' as const },
  disconnected: { label: 'Chưa kết nối', tone: 'neutral' as const },
  unavailable: { label: 'Health Connect không khả dụng', tone: 'danger' as const },
};

function formatDate(value: string | null) {
  if (!value) return 'Chưa đồng bộ';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

export default function HealthConnectScreen() {
  const health = useHealthConnect();
  const status = statusCopy[health.connectionStatus];
  const installRequired = health.snapshot.availability === 'update-required';

  return (
    <AppScreen>
      <ScreenHeader
        title="Google Health Connect"
        description="Đọc dữ liệu sức khỏe bạn cho phép và đồng bộ an toàn với MindCare."
        onBack={() => router.back()}
      />
      {health.loading ? <SkeletonList rows={4} /> : (
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={false} onRefresh={() => void health.refresh()} tintColor={colors.brand} />}
        >
          <View style={styles.heroCard}>
            <View style={styles.logo}><Ionicons color={colors.brandDark} name="fitness" size={28} /></View>
            <View style={styles.heroBody}>
              <Text style={styles.eyebrow}>GOOGLE HEALTH CONNECT</Text>
              <Text style={styles.heroTitle}>Dữ liệu do bạn kiểm soát</Text>
              <View style={[styles.badge, styles[status.tone]]}>
                <View style={[styles.dot, styles[`${status.tone}Dot`]]} />
                <Text style={styles.badgeText}>{status.label}</Text>
              </View>
            </View>
          </View>

          {health.snapshot.availability === 'unsupported-platform' ? (
            <DataFeedback kind="empty" title="Chỉ hỗ trợ Android" description="Health Connect không được tải trên iOS hoặc web. HealthKit sẽ được tích hợp riêng sau." />
          ) : health.snapshot.availability === 'unavailable' ? (
            <DataFeedback kind="error" title="Thiết bị không hỗ trợ" description="Health Connect yêu cầu thiết bị Android tương thích có Google Play services." />
          ) : installRequired ? (
            <DataFeedback actionLabel="Mở Google Play" kind="error" onAction={() => void health.openStore()} title="Cần cài đặt hoặc cập nhật" description="Cài Health Connect trên Android 13 trở xuống, hoặc cập nhật phiên bản hiện có." />
          ) : null}

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Quyền truy cập</Text>
            <Text style={styles.sectionDescription}>MindCare chỉ yêu cầu quyền đọc. Bạn có thể cho phép từng loại dữ liệu.</Text>
            <View style={styles.permissionCard}>
              {permissionLabels.map((item, index) => {
                const granted = health.snapshot.permissions[item.key];
                return (
                  <View key={item.key} style={[styles.permissionRow, index > 0 && styles.rowBorder]}>
                    <View style={styles.permissionIcon}><Ionicons color={colors.brandDark} name={item.icon} size={19} /></View>
                    <Text style={styles.permissionLabel}>{item.label}</Text>
                    <Text style={[styles.permissionState, granted ? styles.grantedText : styles.deniedText]}>
                      {granted ? 'Đã cấp quyền' : 'Chưa cấp quyền'}
                    </Text>
                  </View>
                );
              })}
            </View>
          </View>

          <View style={styles.syncCard}>
            <View>
              <Text style={styles.syncLabel}>Đồng bộ lần cuối</Text>
              <Text style={styles.syncTime}>{formatDate(health.snapshot.lastSyncTime)}</Text>
            </View>
            <Ionicons color={colors.brand} name="sync-circle-outline" size={28} />
          </View>

          {health.error ? <DataFeedback kind="error" title="Không thể hoàn tất" description={health.error} /> : null}
          {health.syncResult ? (
            <View style={styles.resultCard}>
              <Text style={styles.resultTitle}>{health.syncResult.acceptedCount + health.syncResult.duplicateCount === 0 ? 'Không có dữ liệu mới' : 'Đồng bộ hoàn tất'}</Text>
              <Text style={styles.resultText}>
                {health.syncResult.acceptedCount} bản ghi mới · {health.syncResult.duplicateCount} bản ghi trùng
              </Text>
              {health.syncResult.exerciseSkippedCount > 0 ? (
                <Text style={styles.resultNote}>{health.syncResult.exerciseSkippedCount} phiên vận động chưa gửi vì backend chưa hỗ trợ session.</Text>
              ) : null}
              {health.syncResult.invalidSkippedCount > 0 ? (
                <Text style={styles.resultNote}>{health.syncResult.invalidSkippedCount} bản ghi không hợp lệ đã được bỏ qua.</Text>
              ) : null}
            </View>
          ) : null}

          <View style={styles.actions}>
            <ActionButton
              icon="link-outline"
              label={health.connectionStatus === 'disconnected' ? 'Kết nối Health Connect' : 'Cập nhật quyền truy cập'}
              loading={health.action === 'connect'}
              disabled={health.snapshot.availability !== 'available'}
              onPress={() => void health.connect()}
            />
            <ActionButton
              icon="options-outline"
              label="Quản lý quyền"
              tone="secondary"
              disabled={health.snapshot.availability !== 'available'}
              onPress={() => void health.managePermissions()}
            />
            <ActionButton
              icon="sync-outline"
              label="Đồng bộ ngay"
              tone="secondary"
              loading={health.action === 'sync'}
              disabled={health.connectionStatus === 'disconnected' || health.connectionStatus === 'unavailable'}
              onPress={() => void health.sync()}
            />
          </View>

          <Text style={styles.privacyNote}>MindCare không dùng dữ liệu Health Connect để tự động đưa ra chẩn đoán tâm lý.</Text>
        </ScrollView>
      )}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.md, padding: spacing.md, paddingBottom: 48 },
  heroCard: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.md, padding: spacing.md, ...shadows.card },
  logo: { alignItems: 'center', backgroundColor: colors.brandSoft, borderRadius: radius.card, height: 58, justifyContent: 'center', width: 58 },
  heroBody: { flex: 1, gap: spacing.xxs },
  eyebrow: { color: colors.brandDark, fontFamily: fonts.bold, fontSize: 10, letterSpacing: 0.8 },
  heroTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  badge: { alignItems: 'center', alignSelf: 'flex-start', borderRadius: radius.pill, flexDirection: 'row', gap: 6, marginTop: 2, paddingHorizontal: 9, paddingVertical: 5 },
  success: { backgroundColor: colors.mint },
  warning: { backgroundColor: colors.warningSoft },
  neutral: { backgroundColor: colors.surfaceMuted },
  danger: { backgroundColor: colors.dangerSoft },
  dot: { borderRadius: radius.pill, height: 7, width: 7 },
  successDot: { backgroundColor: colors.mintInk },
  warningDot: { backgroundColor: colors.warning },
  neutralDot: { backgroundColor: colors.brandDark },
  dangerDot: { backgroundColor: colors.danger },
  badgeText: { color: colors.inkSoft, fontFamily: fonts.semibold, fontSize: type.caption },
  section: { gap: spacing.xs },
  sectionTitle: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.section },
  sectionDescription: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  permissionCard: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, overflow: 'hidden', ...shadows.card },
  permissionRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, minHeight: 60, paddingHorizontal: spacing.sm },
  rowBorder: { borderTopColor: colors.line, borderTopWidth: 1 },
  permissionIcon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.sm, height: 36, justifyContent: 'center', width: 36 },
  permissionLabel: { color: colors.ink, flex: 1, fontFamily: fonts.medium, fontSize: type.body },
  permissionState: { fontFamily: fonts.semibold, fontSize: type.caption },
  grantedText: { color: colors.mintInk },
  deniedText: { color: colors.muted },
  syncCard: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', justifyContent: 'space-between', padding: spacing.md },
  syncLabel: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  syncTime: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body, marginTop: spacing.xxs },
  resultCard: { backgroundColor: colors.mint, borderRadius: radius.card, gap: spacing.xxs, padding: spacing.md },
  resultTitle: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  resultText: { color: colors.mintInk, fontFamily: fonts.regular, fontSize: type.caption },
  resultNote: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  actions: { gap: spacing.xs },
  privacyNote: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, textAlign: 'center' },
});


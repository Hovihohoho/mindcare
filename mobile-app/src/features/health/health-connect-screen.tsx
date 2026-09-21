/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4
 * genre: modern-minimal · macrostructure: Native Content Flow · design-system: design.md · designed-as-app
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { router } from 'expo-router';
import { Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ActionButton } from '@/components/buttons';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ListItem } from '@/components/list-item';
import { ScreenHeader } from '@/components/screen-header';
import { SectionHeader } from '@/components/section-header';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { HealthMetricItem } from './health-metric-item';
import { HealthTrendChart } from './health-trend-chart';
import type { HealthTrendPoint } from './health.types';
import { useHealthConnect } from './use-health-connect';

const statusCopy = {
  connected: { label: 'Đã kết nối', icon: 'checkmark-circle' as const, tone: 'success' as const },
  partial: { label: 'Thiếu một số quyền', icon: 'alert-circle' as const, tone: 'warning' as const },
  disconnected: { label: 'Chưa kết nối', icon: 'remove-circle-outline' as const, tone: 'neutral' as const },
  unavailable: { label: 'Không khả dụng', icon: 'close-circle' as const, tone: 'danger' as const },
};

function formatDate(value: string | null) {
  if (!value) return 'Chưa đồng bộ';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    month: '2-digit',
  }).format(new Date(value));
}

function dateKey(value: string | Date) {
  return new Intl.DateTimeFormat('en-CA', {
    day: '2-digit',
    month: '2-digit',
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
  }).format(typeof value === 'string' ? new Date(value) : value);
}

function todayPoint(points: HealthTrendPoint[]) {
  const today = dateKey(new Date());
  return points.find((point) => dateKey(point.periodStart) === today);
}

function formatHours(value?: number) {
  if (value === undefined) return '—';
  const hours = Math.floor(value);
  const minutes = Math.round((value - hours) * 60);
  return hours > 0 ? hours + 'h ' + minutes + 'm' : minutes + ' phút';
}

function numberOrDash(value?: number) {
  return value === undefined ? '—' : Math.round(value).toLocaleString('vi-VN');
}

export default function HealthConnectScreen() {
  const health = useHealthConnect();
  const status = statusCopy[health.connectionStatus];
  const installRequired = health.snapshot.availability === 'update-required';
  const connected = health.connectionStatus === 'connected' || health.connectionStatus === 'partial';
  const stepPoints = health.trends.STEP_COUNT ?? [];
  const todaySteps = todayPoint(stepPoints)?.value;
  const todayHeartRate = todayPoint(health.trends.HEART_RATE ?? [])?.value;
  const todaySpO2 = todayPoint(health.trends.SPO2 ?? [])?.value;
  const todaySleep = todayPoint(health.trends.SLEEP_SESSION ?? [])?.value;
  const todayExercise = todayPoint(health.trends.EXERCISE_SESSION ?? [])?.value;
  const recentSteps = stepPoints.filter((point) => dateKey(point.periodStart) !== dateKey(new Date()));
  const recentAverage = recentSteps.length
    ? recentSteps.reduce((sum, point) => sum + point.value, 0) / recentSteps.length
    : undefined;
  const stepChange = todaySteps !== undefined && recentAverage
    ? Math.round(((todaySteps - recentAverage) / recentAverage) * 100)
    : undefined;
  const grantedPermissions = [
    health.snapshot.permissions.steps,
    health.snapshot.permissions.sleep,
    health.snapshot.permissions.heartRate,
    health.snapshot.permissions.restingHeartRate,
    health.snapshot.permissions.oxygenSaturation,
    health.snapshot.permissions.exercise,
  ].filter(Boolean).length;

  const confirmDataPermission = () => Alert.alert(
    'Cho phép đọc dữ liệu sức khỏe?',
    'Android sẽ mở Health Connect để bạn chọn từng loại dữ liệu. MindCare chỉ yêu cầu quyền đọc và bạn có thể thu hồi bất cứ lúc nào.',
    [{ text: 'Để sau', style: 'cancel' }, { text: 'Tiếp tục', onPress: () => void health.connect() }],
  );
  const confirmBackgroundPermission = () => Alert.alert(
    'Bật đồng bộ nền?',
    'Quyền này giúp Android cho phép MindCare cập nhật khi ứng dụng đóng. Lịch chạy phụ thuộc pin và hệ thống nên không đảm bảo cảnh báo tức thời.',
    [{ text: 'Để sau', style: 'cancel' }, { text: 'Mở cấp quyền', onPress: () => void health.enableBackground() }],
  );
  const confirmDeleteStoredData = () => Alert.alert(
    'Xóa dữ liệu đã đồng bộ?',
    'Toàn bộ dữ liệu Health Connect đã lưu trên MindCare sẽ bị xóa vĩnh viễn và đồng bộ nền sẽ được tắt. Hành động này không thể hoàn tác.',
    [
      { text: 'Hủy', style: 'cancel' },
      { text: 'Xóa dữ liệu', style: 'destructive', onPress: () => {
        void health.deleteStoredData().then(() => Alert.alert(
          'Đã xóa dữ liệu trên MindCare',
          'Bạn nên thu hồi quyền trong Health Connect nếu không muốn ứng dụng tiếp tục có quyền đọc trên thiết bị.',
          [{ text: 'Để sau', style: 'cancel' }, { text: 'Quản lý quyền', onPress: () => void health.managePermissions() }],
        )).catch(() => undefined);
      } },
    ],
  );
  const showDetails = () => Alert.alert(
    'Tổng quan hoạt động',
    todaySteps === undefined || recentAverage === undefined
      ? 'Chưa đủ dữ liệu bước chân để so sánh hôm nay với những ngày gần đây.'
      : 'Hôm nay: ' + Math.round(todaySteps).toLocaleString('vi-VN') + ' bước. Trung bình gần đây: ' + Math.round(recentAverage).toLocaleString('vi-VN') + ' bước/ngày.',
  );

  const syncOrConnect = () => {
    if (connected) void health.sync();
    else confirmDataPermission();
  };

  return (
    <AppScreen>
      <ScreenHeader title="Sức khỏe" onBack={() => router.back()} />
      {health.loading ? <SkeletonList rows={4} /> : (
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={false} onRefresh={() => void health.refresh()} tintColor={colors.brand} />}
        >
          <View style={styles.connection}>
            <View style={styles.connectionTop}>
              <View style={styles.connectionBody}>
                <View style={styles.connectionTitleRow}>
                  <Text style={styles.connectionTitle}>Google Health Connect</Text>
                  <Ionicons color={statusColor(status.tone)} name={status.icon} size={19} />
                </View>
                <Text style={[styles.statusText, { color: statusColor(status.tone) }]}>{status.label}</Text>
                <Text style={styles.lastSync}>Đồng bộ lần cuối: {formatDate(health.snapshot.lastSyncTime)}</Text>
              </View>
              <ActionButton
                compact
                disabled={health.snapshot.availability !== 'available' || health.action !== null}
                icon={connected ? 'sync-outline' : 'link-outline'}
                label={connected ? 'Đồng bộ' : 'Kết nối'}
                loading={health.action === 'sync' || health.action === 'connect'}
                onPress={syncOrConnect}
                style={styles.syncButton}
                tone={connected ? 'secondary' : 'primary'}
              />
            </View>
          </View>

          {health.snapshot.availability === 'unsupported-platform' ? (
            <DataFeedback kind="empty" title="Chỉ hỗ trợ Android" description="Health Connect không được tải trên iOS hoặc web. HealthKit sẽ được tích hợp riêng sau." />
          ) : health.snapshot.availability === 'unavailable' ? (
            <DataFeedback kind="error" title="Thiết bị không hỗ trợ" description="Health Connect yêu cầu thiết bị Android tương thích có Google Play services." />
          ) : installRequired ? (
            <DataFeedback actionLabel="Mở Google Play" kind="error" onAction={() => void health.openStore()} title="Cần cài đặt hoặc cập nhật" description="Cài Health Connect trên Android 13 trở xuống, hoặc cập nhật phiên bản hiện có." />
          ) : null}

          {connected ? (
            <>
              <View style={styles.section}>
                <SectionHeader title="Hôm nay" />
                <View style={styles.metricsGrid}>
                  <HealthMetricItem
                    icon="footsteps-outline"
                    index={0}
                    label="Bước chân"
                    note={stepChange === undefined ? undefined : (stepChange >= 0 ? 'Tăng ' : 'Giảm ') + Math.abs(stepChange) + '%'}
                    value={numberOrDash(todaySteps)}
                  />
                  <HealthMetricItem icon="heart-outline" index={1} label="Nhịp tim" note={todayHeartRate === undefined ? undefined : 'Trung bình'} value={todayHeartRate === undefined ? '—' : Math.round(todayHeartRate) + ' bpm'} />
                  <HealthMetricItem icon="water-outline" index={2} label="SpO₂" note={todaySpO2 === undefined ? undefined : 'Trung bình'} value={todaySpO2 === undefined ? '—' : Math.round(todaySpO2) + '%'} />
                  <HealthMetricItem icon="moon-outline" index={3} label="Giấc ngủ" value={formatHours(todaySleep)} />
                  <HealthMetricItem icon="fitness-outline" index={4} label="Hoạt động" note="Hôm nay" value={todayExercise === undefined ? '—' : Math.round(todayExercise * 60) + ' phút'} />
                </View>
              </View>

              <View style={[styles.section, styles.ruledSection]}>
                <SectionHeader title="Xu hướng 7 ngày" />
                <Text style={styles.metricName}>Bước chân</Text>
                <HealthTrendChart points={stepPoints} />
              </View>

              <View style={[styles.section, styles.ruledSection]}>
                <SectionHeader title="Tổng quan" />
                <Text style={styles.insight}>
                  {stepChange === undefined
                    ? 'Chưa đủ dữ liệu để so sánh hoạt động hôm nay với những ngày gần đây.'
                    : stepChange === 0
                      ? 'Hoạt động hôm nay tương đương mức trung bình gần đây.'
                      : 'Hoạt động hôm nay ' + (stepChange > 0 ? 'cao hơn ' : 'thấp hơn ') + Math.abs(stepChange) + '% so với mức trung bình gần đây.'}
                </Text>
                <Pressable accessibilityRole="button" onPress={showDetails} style={({ pressed }) => [styles.detailButton, pressed && styles.pressed]}>
                  <Text style={styles.detailButtonText}>Xem chi tiết</Text>
                  <Ionicons color={colors.brand} name="chevron-forward" size={16} />
                </Pressable>
              </View>
            </>
          ) : null}

          {health.error ? <DataFeedback kind="error" title="Không thể hoàn tất" description={health.error} /> : null}
          {health.syncResult ? (
            <View style={styles.result}>
              <Ionicons color={colors.mintInk} name="checkmark-circle-outline" size={20} />
              <View style={styles.resultBody}>
                <Text style={styles.resultTitle}>{health.syncResult.acceptedCount + health.syncResult.duplicateCount === 0 ? 'Không có dữ liệu mới' : 'Đồng bộ hoàn tất'}</Text>
                <Text style={styles.resultText}>{health.syncResult.acceptedCount} mới · {health.syncResult.updatedCount} cập nhật · {health.syncResult.duplicateCount} không đổi</Text>
                {health.syncResult.invalidSkippedCount > 0 ? <Text style={styles.resultText}>{health.syncResult.invalidSkippedCount} bản ghi không hợp lệ đã được bỏ qua.</Text> : null}
                {health.syncResult.alerts.map((alert) => <Text key={alert.id} style={styles.resultText}>⚠ {alert.triggerReason}</Text>)}
              </View>
            </View>
          ) : null}

          {health.snapshot.availability === 'available' ? (
            <View style={[styles.section, styles.ruledSection]}>
              <SectionHeader title="Quản lý dữ liệu" description="Bạn có thể thay đổi hoặc thu hồi quyền bất cứ lúc nào." />
              <View style={styles.managementList}>
                <ListItem
                  description={grantedPermissions + '/4 loại dữ liệu được phép đọc'}
                  groupStart
                  icon="shield-checkmark-outline"
                  onPress={confirmDataPermission}
                  title={connected ? 'Cập nhật quyền truy cập' : 'Kết nối Health Connect'}
                />
                <ListItem
                  description="Mở cài đặt Health Connect trên thiết bị"
                  icon="options-outline"
                  onPress={() => void health.managePermissions()}
                  title="Quản lý quyền"
                />
                <ListItem
                  description={health.snapshot.permissions.background ? 'Đang bật' : 'Đang tắt'}
                  icon="cloud-upload-outline"
                  onPress={health.snapshot.permissions.background ? () => void health.managePermissions() : confirmBackgroundPermission}
                  title="Đồng bộ nền"
                />
                <ListItem
                  description="Xóa vĩnh viễn dữ liệu Health Connect đang lưu trên MindCare"
                  groupEnd
                  icon="trash-outline"
                  onPress={confirmDeleteStoredData}
                  showDivider={false}
                  title={health.action === 'delete' ? 'Đang xóa dữ liệu…' : 'Xóa dữ liệu đã đồng bộ'}
                />
              </View>
            </View>
          ) : null}

          {!health.snapshot.permissions.background && connected ? (
            <Text style={styles.backgroundNote}>Đồng bộ nền đang tắt. Dữ liệu vẫn được cập nhật khi bạn mở ứng dụng hoặc chọn “Đồng bộ”.</Text>
          ) : null}
          <Text style={styles.privacyNote}>MindCare không dùng dữ liệu Health Connect để tự động đưa ra chẩn đoán tâm lý.</Text>
        </ScrollView>
      )}
    </AppScreen>
  );
}

function statusColor(tone: 'success' | 'warning' | 'neutral' | 'danger') {
  if (tone === 'success') return colors.mintInk;
  if (tone === 'warning') return colors.warning;
  if (tone === 'danger') return colors.danger;
  return colors.muted;
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: spacing.page, paddingBottom: 48 },
  connection: { backgroundColor: colors.mint, borderColor: colors.line, borderLeftColor: colors.brand, borderLeftWidth: 3, borderRadius: radius.card, borderWidth: 1, marginTop: spacing.sm, padding: spacing.md },
  connectionTop: { alignItems: 'center', flexDirection: 'row', gap: spacing.md },
  connectionBody: { flex: 1, minWidth: 0 },
  connectionTitleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs },
  connectionTitle: { color: colors.ink, flexShrink: 1, fontFamily: fonts.semibold, fontSize: type.cardTitle, lineHeight: 23 },
  statusText: { fontFamily: fonts.medium, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
  lastSync: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 2 },
  syncButton: { minWidth: 104 },
  section: { gap: spacing.md, paddingVertical: spacing.xl },
  ruledSection: { borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth },
  metricName: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.label },
  metricsGrid: { backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', flexWrap: 'wrap', overflow: 'hidden' },
  insight: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.body, lineHeight: 24, maxWidth: 340 },
  detailButton: { alignItems: 'center', alignSelf: 'flex-start', borderRadius: radius.sm, flexDirection: 'row', minHeight: 44, paddingRight: spacing.xs },
  detailButtonText: { color: colors.brand, fontFamily: fonts.semibold, fontSize: type.label },
  pressed: { opacity: 0.66 },
  result: { alignItems: 'flex-start', backgroundColor: colors.mint, borderRadius: radius.input, flexDirection: 'row', gap: spacing.xs, marginBottom: spacing.md, padding: spacing.sm },
  resultBody: { flex: 1, minWidth: 0 },
  resultTitle: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.label },
  resultText: { color: colors.mintInk, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 2 },
  managementList: { borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, marginTop: -spacing.xs, overflow: 'hidden' },
  backgroundNote: { color: colors.warning, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, paddingVertical: spacing.sm, textAlign: 'center' },
  privacyNote: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, paddingVertical: spacing.md, textAlign: 'center' },
});

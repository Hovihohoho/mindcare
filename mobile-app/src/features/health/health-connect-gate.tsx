import Ionicons from '@expo/vector-icons/Ionicons';
import { router, usePathname } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { AppState, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/auth-context';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';
import { HEALTH_BANNER_SNOOZE_MS } from './health.constants';
import { updateHealthBackgroundRegistration } from './health-background.task';
import { healthConnectService } from './health-connect.service';
import { healthSyncStorage } from './health.storage';
import { syncHealthIfDue } from './health-sync.coordinator';

type BannerKind = 'connect' | 'background';

export function HealthConnectGate() {
  const { session, status } = useAuth();
  const pathname = usePathname();
  const insets = useSafeAreaInsets();
  const [banner, setBanner] = useState<BannerKind | null>(null);

  const evaluate = useCallback(async (syncWhenDue: boolean) => {
    if (Platform.OS !== 'android' || status !== 'authenticated' || !session) {
      setBanner(null);
      await updateHealthBackgroundRegistration(false).catch(() => undefined);
      return;
    }
    const snapshot = await healthConnectService.getSnapshot(session.user.id);
    if (snapshot.availability !== 'available') {
      setBanner(null);
      return;
    }
    const hasDataPermission = snapshot.permissions.steps || snapshot.permissions.sleep
      || snapshot.permissions.heartRate || snapshot.permissions.restingHeartRate
      || snapshot.permissions.oxygenSaturation || snapshot.permissions.exercise;
    await updateHealthBackgroundRegistration(hasDataPermission && snapshot.permissions.background).catch(() => undefined);
    if (syncWhenDue && hasDataPermission) {
      await syncHealthIfDue(session.user.id, session.accessToken).catch(() => undefined);
    }
    const snoozedUntil = await healthSyncStorage.getBannerSnoozedUntil(session.user.id);
    setBanner(snoozedUntil > Date.now() ? null : !hasDataPermission ? 'connect' : !snapshot.permissions.background ? 'background' : null);
  }, [session, status]);

  useEffect(() => { void evaluate(true).catch(() => setBanner(null)); }, [evaluate]);
  useEffect(() => {
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') void evaluate(true).catch(() => setBanner(null));
    });
    return () => subscription.remove();
  }, [evaluate]);

  if (!banner || pathname.includes('health-connect')) return null;
  const background = banner === 'background';
  return (
    <View pointerEvents="box-none" style={[styles.host, { top: insets.top + spacing.xs }]}>
      <View style={styles.banner}>
        <Ionicons color={colors.brandDark} name={background ? 'cloud-offline-outline' : 'fitness-outline'} size={22} />
        <Pressable style={styles.body} onPress={() => router.push('/(tabs)/health-connect')}>
          <Text style={styles.title}>{background ? 'Theo dõi liên tục đang tắt' : 'Kết nối dữ liệu sức khỏe'}</Text>
          <Text style={styles.copy}>{background ? 'Dữ liệu vẫn cập nhật khi bạn mở ứng dụng.' : 'Cho phép MindCare đọc dữ liệu bạn chọn.'}</Text>
        </Pressable>
        <Pressable
          accessibilityLabel="Nhắc lại sau"
          hitSlop={10}
          onPress={() => {
            setBanner(null);
            if (session) void healthSyncStorage.setBannerSnoozedUntil(session.user.id, Date.now() + HEALTH_BANNER_SNOOZE_MS);
          }}
        >
          <Ionicons color={colors.muted} name="close" size={20} />
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  host: { left: spacing.sm, position: 'absolute', right: spacing.sm, zIndex: 1000 },
  banner: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, padding: spacing.sm, ...shadows.card },
  body: { flex: 1 },
  title: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.body },
  copy: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginTop: 2 },
});

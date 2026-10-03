import { Platform } from 'react-native';

import { ensurePushRegistered, loadNotifications } from '@/features/notifications/push-registration';

export const STRESS_NOTIFICATION_CHANNEL = 'stress-insights';

export async function hasStressNotificationPermission(): Promise<boolean> {
  if (Platform.OS === 'web') return false;
  const Notifications = await loadNotifications();
  return (await Notifications.getPermissionsAsync()).status === 'granted';
}

export async function ensureStressNotificationPermission(token: string): Promise<boolean> {
  if (Platform.OS === 'web') return false;
  const Notifications = await loadNotifications();
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync(STRESS_NOTIFICATION_CHANNEL, {
      name: 'Đánh giá sức khỏe PMData',
      description: 'Thông báo khi MindCare hoàn tất đánh giá dữ liệu sức khỏe trong ngày.',
      importance: Notifications.AndroidImportance.HIGH,
      lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
      vibrationPattern: [0, 250, 150, 250],
    });
  }
  await ensurePushRegistered(token);
  return true;
}

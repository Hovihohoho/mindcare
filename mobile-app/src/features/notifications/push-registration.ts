import Constants, { ExecutionEnvironment } from 'expo-constants';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { apiRequest } from '@/services/api/api.client';

const INSTALLATION_KEY = 'mindcare.push.installation-id';
type NotificationsModule = typeof import('expo-notifications');

let notificationsPromise: Promise<NotificationsModule> | undefined;

function newId() { return globalThis.crypto?.randomUUID?.() ?? `device-${Date.now()}-${Math.random().toString(36).slice(2)}`; }
async function installationId() { const stored = await SecureStore.getItemAsync(INSTALLATION_KEY); if (stored) return stored; const value = newId(); await SecureStore.setItemAsync(INSTALLATION_KEY, value); return value; }

export function canUsePushNotifications() {
  return Platform.OS !== 'web' && Constants.executionEnvironment !== ExecutionEnvironment.StoreClient;
}

export function canUseLocalNotifications() {
  return Platform.OS !== 'web';
}

export async function loadNotifications() {
  if (!canUseLocalNotifications()) throw new Error('NOTIFICATIONS_UNAVAILABLE');
  notificationsPromise ??= import('expo-notifications')
    .then((Notifications) => {
      Notifications.setNotificationHandler({
        handleNotification: async () => ({ shouldPlaySound: false, shouldSetBadge: false, shouldShowBanner: true, shouldShowList: true }),
      });
      return Notifications;
    })
    .catch(() => {
      notificationsPromise = undefined;
      throw new Error('PUSH_NATIVE_MODULE_MISSING');
    });
  return notificationsPromise;
}

export async function ensurePushRegistered(token: string) {
  if (!canUsePushNotifications()) throw new Error('PUSH_REQUIRES_DEVELOPMENT_BUILD');
  const Notifications = await loadNotifications();
  if (Platform.OS === 'android') {
    await Promise.all([
      Notifications.setNotificationChannelAsync('reminders', {
        name: 'Nhắc nhở',
        importance: Notifications.AndroidImportance.DEFAULT,
      }),
      Notifications.setNotificationChannelAsync('stress-insights', {
        name: 'Cảnh báo sức khỏe',
        description: 'Cảnh báo PMData mức 4–5 và tín hiệu sức khỏe theo benchmark.',
        importance: Notifications.AndroidImportance.HIGH,
        lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
        vibrationPattern: [0, 250, 150, 250],
      }),
    ]);
  }
  let permission = await Notifications.getPermissionsAsync();
  if (permission.status !== 'granted') permission = await Notifications.requestPermissionsAsync();
  if (permission.status !== 'granted') throw new Error('PUSH_PERMISSION_DENIED');
  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) throw new Error('EXPO_PROJECT_ID_MISSING');
  const pushToken = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  await apiRequest('/api/v1/push-devices', { method: 'PUT', token, body: { installationId: await installationId(), pushToken, platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' } });
}

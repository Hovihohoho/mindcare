import Constants from 'expo-constants';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { apiRequest } from '@/services/api/api.client';

const INSTALLATION_KEY = 'mindcare.push.installation-id';
function newId() { return globalThis.crypto?.randomUUID?.() ?? `device-${Date.now()}-${Math.random().toString(36).slice(2)}`; }
async function installationId() { const stored = await SecureStore.getItemAsync(INSTALLATION_KEY); if (stored) return stored; const value = newId(); await SecureStore.setItemAsync(INSTALLATION_KEY, value); return value; }

export async function ensurePushRegistered(token: string) {
  if (Platform.OS === 'web') return;
  if (Platform.OS === 'android') await Notifications.setNotificationChannelAsync('reminders', { name: 'Nhắc nhở', importance: Notifications.AndroidImportance.DEFAULT });
  let permission = await Notifications.getPermissionsAsync();
  if (permission.status !== 'granted') permission = await Notifications.requestPermissionsAsync();
  if (permission.status !== 'granted') throw new Error('PUSH_PERMISSION_DENIED');
  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) throw new Error('EXPO_PROJECT_ID_MISSING');
  const pushToken = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  await apiRequest('/api/v1/push-devices', { method: 'PUT', token, body: { installationId: await installationId(), pushToken, platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' } });
}

Notifications.setNotificationHandler({ handleNotification: async () => ({ shouldPlaySound: false, shouldSetBadge: false, shouldShowBanner: true, shouldShowList: true }) });

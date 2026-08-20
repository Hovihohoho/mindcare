import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const LAST_SYNC_KEY_PREFIX = 'mindcare.health.last-sync.v1';
const options: SecureStore.SecureStoreOptions = {
  keychainService: 'mindcare.health.sync',
};

function keyFor(userId: string) {
  return `${LAST_SYNC_KEY_PREFIX}.${userId}`;
}

function webStorage() {
  return typeof globalThis !== 'undefined' && 'localStorage' in globalThis
    ? globalThis.localStorage
    : undefined;
}

export const healthSyncStorage = {
  async getLastSyncTime(userId: string): Promise<string | null> {
    const value = Platform.OS === 'web'
      ? webStorage()?.getItem(keyFor(userId)) ?? null
      : await SecureStore.getItemAsync(keyFor(userId), options);
    if (!value || Number.isNaN(Date.parse(value))) return null;
    return value;
  },

  async setLastSyncTime(userId: string, value: string): Promise<void> {
    if (Platform.OS === 'web') {
      webStorage()?.setItem(keyFor(userId), value);
      return;
    }
    await SecureStore.setItemAsync(keyFor(userId), value, options);
  },
};

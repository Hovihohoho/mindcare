import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { AuthSession } from '@/features/auth/auth.types';

const SESSION_KEY = 'mindcare.session.v1';
const secureStoreOptions: SecureStore.SecureStoreOptions = {
  keychainService: 'mindcare.auth.session',
};

function isSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') return false;
  const session = value as Partial<AuthSession>;
  return typeof session.accessToken === 'string'
    && session.tokenType === 'Bearer'
    && typeof session.expiresIn === 'number'
    && typeof session.expiresAt === 'number'
    && typeof session.user?.id === 'string'
    && typeof session.user?.email === 'string'
    && typeof session.user?.fullName === 'string';
}

function getWebStorage() {
  return typeof globalThis !== 'undefined' && 'localStorage' in globalThis
    ? globalThis.localStorage
    : undefined;
}

export const sessionStorage = {
  async load(): Promise<AuthSession | null> {
    const raw = Platform.OS === 'web'
      ? getWebStorage()?.getItem(SESSION_KEY) ?? null
      : await SecureStore.getItemAsync(SESSION_KEY, secureStoreOptions);

    if (!raw) return null;

    try {
      const session: unknown = JSON.parse(raw);
      if (isSession(session) && session.expiresAt > Date.now()) return session;
    } catch {
      // Corrupt or stale local data should never lock the user out of the auth flow.
    }

    await this.clear();
    return null;
  },

  async save(session: AuthSession): Promise<void> {
    const value = JSON.stringify(session);
    if (Platform.OS === 'web') {
      getWebStorage()?.setItem(SESSION_KEY, value);
      return;
    }
    await SecureStore.setItemAsync(SESSION_KEY, value, secureStoreOptions);
  },

  async clear(): Promise<void> {
    if (Platform.OS === 'web') {
      getWebStorage()?.removeItem(SESSION_KEY);
      return;
    }
    await SecureStore.deleteItemAsync(SESSION_KEY, secureStoreOptions);
  },
};

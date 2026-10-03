import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { DatedStressPrediction } from './stress.types';

const LATEST_PREDICTION_KEY_PREFIX = 'mindcare.pmdata.latest-prediction.v1';
const options: SecureStore.SecureStoreOptions = {
  keychainService: 'mindcare.pmdata.prediction',
};

function latestKey(userId: string) {
  return `${LATEST_PREDICTION_KEY_PREFIX}.${userId}`;
}

function webStorage() {
  return typeof globalThis !== 'undefined' && 'localStorage' in globalThis
    ? globalThis.localStorage
    : undefined;
}

async function getItem(key: string) {
  return Platform.OS === 'web'
    ? webStorage()?.getItem(key) ?? null
    : SecureStore.getItemAsync(key, options);
}

async function setItem(key: string, value: string) {
  if (Platform.OS === 'web') {
    webStorage()?.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value, options);
}

async function deleteItem(key: string) {
  if (Platform.OS === 'web') {
    webStorage()?.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key, options);
}

export const stressStorage = {
  async getLatest(userId: string): Promise<DatedStressPrediction | null> {
    const raw = await getItem(latestKey(userId));
    if (!raw) return null;
    try {
      return JSON.parse(raw) as DatedStressPrediction;
    } catch {
      await deleteItem(latestKey(userId));
      return null;
    }
  },

  saveLatest(userId: string, prediction: DatedStressPrediction) {
    return setItem(latestKey(userId), JSON.stringify(prediction));
  },

  async clear(userId: string) {
    await deleteItem(latestKey(userId));
  },
};

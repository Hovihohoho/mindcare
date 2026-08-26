import { FOREGROUND_SYNC_INTERVAL_MS } from './health.constants';
import { healthConnectService } from './health-connect.service';
import { healthSyncStorage } from './health.storage';
import type { HealthSyncResult } from './health.types';

let activeSync: Promise<HealthSyncResult> | null = null;

export async function syncHealthIfDue(
  userId: string,
  token: string,
  options: { force?: boolean; minimumAgeMs?: number } = {},
): Promise<HealthSyncResult | null> {
  const lastSync = await healthSyncStorage.getLastSyncTime(userId);
  const minimumAge = options.minimumAgeMs ?? FOREGROUND_SYNC_INTERVAL_MS;
  if (!options.force && lastSync && Date.now() - Date.parse(lastSync) < minimumAge) return null;
  if (activeSync) return activeSync;

  activeSync = healthConnectService.syncHealthData(userId, token);
  try {
    return await activeSync;
  } finally {
    activeSync = null;
  }
}

import { FOREGROUND_SYNC_INTERVAL_MS } from './health.constants';
import { predictLatestPmdataStress } from '@/features/stress/stress-prediction.coordinator';
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

  activeSync = healthConnectService.syncHealthData(userId, token).then(async (result) => {
    try {
      result.stressPrediction = await predictLatestPmdataStress(userId, token);
    } catch {
      result.stressPrediction = null;
      result.stressPredictionError = 'Không thể tạo đánh giá PMData. Hãy kiểm tra ai-service và model stress.';
    }
    return result;
  });
  try {
    return await activeSync;
  } finally {
    activeSync = null;
  }
}

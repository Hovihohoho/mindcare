import * as BackgroundTask from 'expo-background-task';
import * as TaskManager from 'expo-task-manager';
import { Platform } from 'react-native';

import { sessionStorage } from '@/services/auth/session.storage';
import { BACKGROUND_SYNC_INTERVAL_MINUTES } from './health.constants';
import { healthConnectService } from './health-connect.service';
import { syncHealthIfDue } from './health-sync.coordinator';

export const HEALTH_BACKGROUND_TASK = 'mindcare-health-connect-sync-v1';

if (Platform.OS === 'android' && !TaskManager.isTaskDefined(HEALTH_BACKGROUND_TASK)) {
  TaskManager.defineTask(HEALTH_BACKGROUND_TASK, async () => {
    try {
      const session = await sessionStorage.load();
      if (!session) return BackgroundTask.BackgroundTaskResult.Success;
      const snapshot = await healthConnectService.getSnapshot(session.user.id);
      if (!snapshot.permissions.background) return BackgroundTask.BackgroundTaskResult.Success;
      await syncHealthIfDue(session.user.id, session.accessToken, {
        minimumAgeMs: BACKGROUND_SYNC_INTERVAL_MINUTES * 60_000,
      });
      return BackgroundTask.BackgroundTaskResult.Success;
    } catch {
      return BackgroundTask.BackgroundTaskResult.Failed;
    }
  });
}

export async function updateHealthBackgroundRegistration(enabled: boolean) {
  if (Platform.OS !== 'android' || !(await TaskManager.isAvailableAsync())) return;
  const registered = await TaskManager.isTaskRegisteredAsync(HEALTH_BACKGROUND_TASK);
  if (enabled && !registered) {
    await BackgroundTask.registerTaskAsync(HEALTH_BACKGROUND_TASK, {
      minimumInterval: BACKGROUND_SYNC_INTERVAL_MINUTES,
    });
  } else if (!enabled && registered) {
    await BackgroundTask.unregisterTaskAsync(HEALTH_BACKGROUND_TASK);
  }
}

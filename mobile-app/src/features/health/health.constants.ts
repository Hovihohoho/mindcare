import type { Permission } from 'react-native-health-connect';
import type { HealthDataType, HealthPermissionState } from './health.types';

export const HEALTH_PERMISSIONS: Permission[] = [
  { accessType: 'read', recordType: 'Steps' },
  { accessType: 'read', recordType: 'SleepSession' },
  { accessType: 'read', recordType: 'HeartRate' },
  { accessType: 'read', recordType: 'RestingHeartRate' },
  { accessType: 'read', recordType: 'OxygenSaturation' },
  { accessType: 'read', recordType: 'ExerciseSession' },
];

export const HEALTH_RECORD_TYPES: Record<HealthDataType, Permission['recordType']> = {
  steps: 'Steps',
  sleep: 'SleepSession',
  heartRate: 'HeartRate',
  restingHeartRate: 'RestingHeartRate',
  oxygenSaturation: 'OxygenSaturation',
  exercise: 'ExerciseSession',
};

export const EMPTY_HEALTH_PERMISSIONS: HealthPermissionState = {
  background: false,
  steps: false,
  sleep: false,
  heartRate: false,
  restingHeartRate: false,
  oxygenSaturation: false,
  exercise: false,
};

export const HEALTH_BACKGROUND_PERMISSION = {
  accessType: 'read' as const,
  recordType: 'BackgroundAccessPermission' as const,
};

export const INITIAL_SYNC_DAYS = 7;
export const HEALTH_SYNC_BATCH_SIZE = 100;
export const FOREGROUND_SYNC_INTERVAL_MS = 60 * 60 * 1000;
export const BACKGROUND_SYNC_INTERVAL_MINUTES = 120;
export const HEALTH_BANNER_SNOOZE_MS = 24 * 60 * 60 * 1000;

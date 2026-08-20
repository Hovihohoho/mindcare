import type { Permission } from 'react-native-health-connect';
import type { HealthDataType, HealthPermissionState } from './health.types';

export const HEALTH_PERMISSIONS: Permission[] = [
  { accessType: 'read', recordType: 'Steps' },
  { accessType: 'read', recordType: 'SleepSession' },
  { accessType: 'read', recordType: 'HeartRate' },
  { accessType: 'read', recordType: 'ExerciseSession' },
];

export const HEALTH_RECORD_TYPES: Record<HealthDataType, Permission['recordType']> = {
  steps: 'Steps',
  sleep: 'SleepSession',
  heartRate: 'HeartRate',
  exercise: 'ExerciseSession',
};

export const EMPTY_HEALTH_PERMISSIONS: HealthPermissionState = {
  steps: false,
  sleep: false,
  heartRate: false,
  exercise: false,
};

export const INITIAL_SYNC_DAYS = 7;
export const HEALTH_SYNC_BATCH_SIZE = 100;


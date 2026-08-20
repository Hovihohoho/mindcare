import { Linking, Platform } from 'react-native';
import type {
  ReadRecordsOptions,
  RecordResult,
  RecordType,
} from 'react-native-health-connect';

import { healthApi } from './health.api';
import {
  EMPTY_HEALTH_PERMISSIONS,
  HEALTH_PERMISSIONS,
  HEALTH_RECORD_TYPES,
  HEALTH_SYNC_BATCH_SIZE,
  INITIAL_SYNC_DAYS,
} from './health.constants';
import { healthSyncStorage } from './health.storage';
import type {
  ExerciseHealthRecord,
  HealthConnectAvailability,
  HealthConnectSnapshot,
  HealthDataType,
  HealthMetricSyncItem,
  HealthPermissionState,
  HealthSource,
  HealthSyncResult,
  HeartRateHealthRecord,
  NormalizedHealthData,
  SleepHealthRecord,
  StepHealthRecord,
} from './health.types';

type HealthConnectModule = typeof import('react-native-health-connect');

export class HealthConnectError extends Error {
  constructor(
    readonly code:
      | 'UNSUPPORTED_PLATFORM'
      | 'UNAVAILABLE'
      | 'UPDATE_REQUIRED'
      | 'INITIALIZATION_FAILED'
      | 'NO_PERMISSIONS'
      | 'READ_FAILED',
    message: string,
    readonly cause?: unknown,
  ) {
    super(message);
    this.name = 'HealthConnectError';
  }
}

function debug(event: string, details?: unknown) {
  if (__DEV__) console.info(`[HealthConnect] ${event}`, details ?? '');
}

async function nativeModule(): Promise<HealthConnectModule> {
  if (Platform.OS !== 'android') {
    throw new HealthConnectError('UNSUPPORTED_PLATFORM', 'Health Connect chỉ khả dụng trên Android.');
  }
  return import('react-native-health-connect');
}

function permissionState(permissions: readonly { accessType: string; recordType: string }[]): HealthPermissionState {
  return (Object.keys(HEALTH_RECORD_TYPES) as HealthDataType[]).reduce(
    (state, key) => ({
      ...state,
      [key]: permissions.some((item) => item.accessType === 'read' && item.recordType === HEALTH_RECORD_TYPES[key]),
    }),
    { ...EMPTY_HEALTH_PERMISSIONS },
  );
}

function sourceOf(metadata?: {
  dataOrigin?: string;
  device?: { manufacturer?: string; model?: string };
}): HealthSource {
  const deviceName = [metadata?.device?.manufacturer, metadata?.device?.model].filter(Boolean).join(' ').trim();
  return {
    origin: metadata?.dataOrigin,
    name: deviceName || metadata?.dataOrigin,
  };
}

function externalId(prefix: string, metadataId: string | undefined, fallback: string) {
  return `${prefix}:${metadataId || fallback}`.slice(0, 255);
}

async function readAll<T extends RecordType>(
  module: HealthConnectModule,
  recordType: T,
  startTime: string,
  endTime: string,
): Promise<RecordResult<T>[]> {
  const records: RecordResult<T>[] = [];
  let pageToken: string | undefined;
  do {
    const options: ReadRecordsOptions = {
      timeRangeFilter: { operator: 'between', startTime, endTime },
      ascendingOrder: true,
      pageSize: 1000,
      ...(pageToken ? { pageToken } : {}),
    };
    const page = await module.readRecords(recordType, options);
    records.push(...page.records);
    pageToken = page.pageToken;
  } while (pageToken);
  return records;
}

function emptyData(): NormalizedHealthData {
  return { steps: [], sleep: [], heartRate: [], exercise: [] };
}

function toBackendItems(data: NormalizedHealthData) {
  let invalidSkippedCount = 0;
  const items: HealthMetricSyncItem[] = [];

  data.steps.forEach((record) => {
    if (!Number.isInteger(record.count) || record.count < 0 || record.count > 200_000) {
      invalidSkippedCount += 1;
      return;
    }
    items.push({
      externalSampleId: record.recordId,
      metricType: 'STEP_COUNT',
      recordedAt: record.endTime,
      unit: 'count',
      value: record.count,
    });
  });

  data.heartRate.forEach((record) => {
    if (!Number.isFinite(record.beatsPerMinute) || record.beatsPerMinute < 20 || record.beatsPerMinute > 250) {
      invalidSkippedCount += 1;
      return;
    }
    items.push({
      externalSampleId: record.recordId,
      metricType: 'HEART_RATE',
      recordedAt: record.time,
      unit: 'bpm',
      value: record.beatsPerMinute,
    });
  });

  data.sleep.forEach((record) => {
    const hours = (Date.parse(record.endTime) - Date.parse(record.startTime)) / 3_600_000;
    if (!Number.isFinite(hours) || hours <= 0 || hours > 24) {
      invalidSkippedCount += 1;
      return;
    }
    items.push({
      externalSampleId: record.recordId,
      metricType: 'SLEEP_HOURS',
      recordedAt: record.endTime,
      unit: 'h',
      value: Math.round(hours * 100) / 100,
    });
  });

  return { invalidSkippedCount, items };
}

async function ensureAvailable(module: HealthConnectModule): Promise<void> {
  const status = await module.getSdkStatus();
  debug('availability', status);
  if (status === module.SdkAvailabilityStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
    throw new HealthConnectError('UPDATE_REQUIRED', 'Health Connect cần được cài đặt hoặc cập nhật.');
  }
  if (status !== module.SdkAvailabilityStatus.SDK_AVAILABLE) {
    throw new HealthConnectError('UNAVAILABLE', 'Thiết bị này không hỗ trợ Health Connect.');
  }
}

export const healthConnectService = {
  async checkHealthConnectAvailability(): Promise<HealthConnectAvailability> {
    if (Platform.OS !== 'android') return 'unsupported-platform';
    try {
      const module = await nativeModule();
      const status = await module.getSdkStatus();
      debug('availability', status);
      if (status === module.SdkAvailabilityStatus.SDK_AVAILABLE) return 'available';
      if (status === module.SdkAvailabilityStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) return 'update-required';
      return 'unavailable';
    } catch (error) {
      debug('availability error', error);
      return 'unavailable';
    }
  },

  async initializeHealthConnect(): Promise<boolean> {
    const module = await nativeModule();
    await ensureAvailable(module);
    const initialized = await module.initialize();
    if (!initialized) {
      throw new HealthConnectError('INITIALIZATION_FAILED', 'Không thể khởi tạo Health Connect.');
    }
    return true;
  },

  async getGrantedPermissions(): Promise<HealthPermissionState> {
    const module = await nativeModule();
    await ensureAvailable(module);
    await module.initialize();
    const granted = await module.getGrantedPermissions();
    debug('granted permissions', granted.map((item) => item.recordType));
    return permissionState(granted);
  },

  async requestHealthPermissions(): Promise<HealthPermissionState> {
    const module = await nativeModule();
    await ensureAvailable(module);
    await module.initialize();
    debug('requested permissions', HEALTH_PERMISSIONS.map((item) => item.recordType));
    await module.requestPermission(HEALTH_PERMISSIONS);
    const granted = await module.getGrantedPermissions();
    debug('granted permissions', granted.map((item) => item.recordType));
    return permissionState(granted);
  },

  async openHealthConnectSettings(): Promise<void> {
    const module = await nativeModule();
    module.openHealthConnectSettings();
  },

  async openHealthConnectStore(): Promise<void> {
    if (Platform.OS !== 'android') return;
    const marketUrl = 'market://details?id=com.google.android.apps.healthdata';
    const webUrl = 'https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata';
    try {
      await Linking.openURL(marketUrl);
    } catch {
      await Linking.openURL(webUrl);
    }
  },

  async readSteps(startTime: string, endTime: string): Promise<StepHealthRecord[]> {
    const module = await nativeModule();
    const records = await readAll(module, 'Steps', startTime, endTime);
    return records.map((record) => ({
      metricType: 'STEPS',
      count: record.count,
      startTime: record.startTime,
      endTime: record.endTime,
      recordId: externalId('steps', record.metadata?.id, `${record.metadata?.dataOrigin}:${record.startTime}:${record.endTime}`),
      source: sourceOf(record.metadata),
    }));
  },

  async readSleep(startTime: string, endTime: string): Promise<SleepHealthRecord[]> {
    const module = await nativeModule();
    const records = await readAll(module, 'SleepSession', startTime, endTime);
    return records.map((record) => ({
      metricType: 'SLEEP',
      startTime: record.startTime,
      endTime: record.endTime,
      stages: record.stages ?? [],
      recordId: externalId('sleep', record.metadata?.id, `${record.metadata?.dataOrigin}:${record.startTime}:${record.endTime}`),
      source: sourceOf(record.metadata),
    }));
  },

  async readHeartRate(startTime: string, endTime: string): Promise<HeartRateHealthRecord[]> {
    const module = await nativeModule();
    const records = await readAll(module, 'HeartRate', startTime, endTime);
    return records.flatMap((record) => record.samples.map((sample) => ({
      metricType: 'HEART_RATE' as const,
      beatsPerMinute: sample.beatsPerMinute,
      time: sample.time,
      startTime: record.startTime,
      endTime: record.endTime,
      recordId: externalId('heart-rate', undefined, `${record.metadata?.id || record.metadata?.dataOrigin}:${sample.time}`),
      source: sourceOf(record.metadata),
    })));
  },

  async readExercise(startTime: string, endTime: string): Promise<ExerciseHealthRecord[]> {
    const module = await nativeModule();
    const records = await readAll(module, 'ExerciseSession', startTime, endTime);
    return records.map((record) => ({
      metricType: 'EXERCISE',
      exerciseType: record.exerciseType,
      startTime: record.startTime,
      endTime: record.endTime,
      recordId: externalId('exercise', record.metadata?.id, `${record.metadata?.dataOrigin}:${record.startTime}:${record.endTime}`),
      source: sourceOf(record.metadata),
    }));
  },

  async getSnapshot(userId: string): Promise<HealthConnectSnapshot> {
    const availability = await this.checkHealthConnectAvailability();
    const lastSyncTime = await healthSyncStorage.getLastSyncTime(userId);
    if (availability !== 'available') {
      return { availability, initialized: false, lastSyncTime, permissions: { ...EMPTY_HEALTH_PERMISSIONS } };
    }
    try {
      await this.initializeHealthConnect();
      const permissions = await this.getGrantedPermissions();
      return { availability, initialized: true, lastSyncTime, permissions };
    } catch (error) {
      debug('snapshot error', error);
      return { availability: 'unavailable', initialized: false, lastSyncTime, permissions: { ...EMPTY_HEALTH_PERMISSIONS } };
    }
  },

  async syncHealthData(userId: string, token: string): Promise<HealthSyncResult> {
    await this.initializeHealthConnect();
    const permissions = await this.getGrantedPermissions();
    if (!Object.values(permissions).some(Boolean)) {
      throw new HealthConnectError('NO_PERMISSIONS', 'MindCare chưa có quyền đọc dữ liệu Health Connect.');
    }

    const endTime = new Date().toISOString();
    const persistedStart = await healthSyncStorage.getLastSyncTime(userId);
    const startTime = persistedStart ?? new Date(Date.now() - INITIAL_SYNC_DAYS * 86_400_000).toISOString();
    debug('read range', { startTime, endTime });

    const data = emptyData();
    try {
      if (permissions.steps) data.steps = await this.readSteps(startTime, endTime);
      if (permissions.sleep) data.sleep = await this.readSleep(startTime, endTime);
      if (permissions.heartRate) data.heartRate = await this.readHeartRate(startTime, endTime);
      if (permissions.exercise) data.exercise = await this.readExercise(startTime, endTime);
    } catch (error) {
      throw new HealthConnectError('READ_FAILED', 'Health Connect không thể đọc dữ liệu trong khoảng đồng bộ.', error);
    }

    const readCounts = {
      steps: data.steps.length,
      sleep: data.sleep.length,
      heartRate: data.heartRate.length,
      exercise: data.exercise.length,
    };
    debug('records returned', readCounts);

    const { invalidSkippedCount, items } = toBackendItems(data);
    let acceptedCount = 0;
    let duplicateCount = 0;
    for (let offset = 0; offset < items.length; offset += HEALTH_SYNC_BATCH_SIZE) {
      const batch = items.slice(offset, offset + HEALTH_SYNC_BATCH_SIZE);
      const response = await healthApi.syncMetrics(
        token,
        batch,
        `health-connect:${startTime}:${endTime}:${offset / HEALTH_SYNC_BATCH_SIZE}`,
      );
      acceptedCount += response.acceptedCount;
      duplicateCount += response.duplicateCount;
    }

    await healthSyncStorage.setLastSyncTime(userId, endTime);
    const result = {
      acceptedCount,
      duplicateCount,
      exerciseSkippedCount: data.exercise.length,
      invalidSkippedCount,
      readCounts,
      syncedAt: endTime,
    };
    debug('sync result', result);
    return result;
  },
};

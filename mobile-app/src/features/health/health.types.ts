export type HealthDataType = 'steps' | 'sleep' | 'heartRate' | 'exercise';

export type HealthConnectAvailability =
  | 'available'
  | 'unsupported-platform'
  | 'unavailable'
  | 'update-required';

export type HealthPermissionState = Record<HealthDataType, boolean>;

export type HealthSource = {
  origin?: string;
  name?: string;
};

type HealthRecordBase = {
  recordId: string;
  startTime: string;
  endTime: string;
  source: HealthSource;
};

export type StepHealthRecord = HealthRecordBase & {
  metricType: 'STEPS';
  count: number;
};

export type SleepStage = {
  endTime: string;
  stage: number;
  startTime: string;
};

export type SleepHealthRecord = HealthRecordBase & {
  metricType: 'SLEEP';
  stages: SleepStage[];
};

export type HeartRateHealthRecord = HealthRecordBase & {
  metricType: 'HEART_RATE';
  beatsPerMinute: number;
  time: string;
};

export type ExerciseHealthRecord = HealthRecordBase & {
  metricType: 'EXERCISE';
  exerciseType: number;
};

export type NormalizedHealthData = {
  exercise: ExerciseHealthRecord[];
  heartRate: HeartRateHealthRecord[];
  sleep: SleepHealthRecord[];
  steps: StepHealthRecord[];
};

export type HealthMetricSyncItem = {
  externalSampleId: string;
  metricType: 'STEP_COUNT' | 'HEART_RATE' | 'SLEEP_HOURS';
  recordedAt: string;
  unit: 'count' | 'bpm' | 'h';
  value: number;
};

export type HealthMetricBatchResponse = {
  acceptedCount: number;
  duplicateCount: number;
  processedMetricIds: string[];
};

export type HealthSyncResult = {
  acceptedCount: number;
  duplicateCount: number;
  exerciseSkippedCount: number;
  invalidSkippedCount: number;
  readCounts: Record<HealthDataType, number>;
  syncedAt: string;
};

export type HealthConnectSnapshot = {
  availability: HealthConnectAvailability;
  initialized: boolean;
  lastSyncTime: string | null;
  permissions: HealthPermissionState;
};

export type HealthDataType = 'steps' | 'sleep' | 'heartRate' | 'restingHeartRate' | 'oxygenSaturation' | 'exercise';

export type HealthConnectAvailability =
  | 'available'
  | 'unsupported-platform'
  | 'unavailable'
  | 'update-required';

export type HealthPermissionState = Record<HealthDataType, boolean> & { background: boolean };

export type HealthSource = {
  origin?: string;
  name?: string;
};

type HealthRecordBase = {
  recordId: string;
  startTime: string;
  endTime: string;
  source: HealthSource;
  sourceLastModifiedAt?: string;
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
  resting?: boolean;
};

export type OxygenSaturationHealthRecord = HealthRecordBase & {
  metricType: 'OXYGEN_SATURATION';
  percentage: number;
  time: string;
};

export type ExerciseHealthRecord = HealthRecordBase & {
  metricType: 'EXERCISE';
  exerciseType: number;
};

export type NormalizedHealthData = {
  exercise: ExerciseHealthRecord[];
  heartRate: HeartRateHealthRecord[];
  oxygenSaturation: OxygenSaturationHealthRecord[];
  sleep: SleepHealthRecord[];
  steps: StepHealthRecord[];
};

export type HealthMetricSyncItem = {
  externalSampleId: string;
  metricType: 'STEP_COUNT' | 'HEART_RATE' | 'SPO2' | 'SLEEP_SESSION' | 'EXERCISE_SESSION';
  recordedAt: string;
  unit?: 'count' | 'bpm' | '%';
  value?: number;
  startTime?: string;
  endTime?: string;
  sourceName?: string;
  dataOrigin?: string;
  sourceLastModifiedAt?: string;
  details?: Record<string, unknown>;
};

export type HealthMetricBatchResponse = {
  acceptedCount: number;
  duplicateCount: number;
  updatedCount: number;
  processedMetricIds: string[];
};

export type HealthSyncResult = {
  acceptedCount: number;
  duplicateCount: number;
  updatedCount: number;
  invalidSkippedCount: number;
  readCounts: Record<HealthDataType, number>;
  syncedAt: string;
  alerts: HealthBenchmarkAlert[];
};

export type HealthBenchmarkAlert = {
  id: string;
  alertLevel: string;
  triggerReason: string;
  metricType?: string;
  observedValue?: number;
  observedUnit?: string;
  recommendedPlanTemplateCode?: string;
};

export type HealthConnectSnapshot = {
  availability: HealthConnectAvailability;
  initialized: boolean;
  lastSyncTime: string | null;
  permissions: HealthPermissionState;
};

export type HealthTrendPoint = {
  periodStart: string;
  periodEnd: string;
  metricType: HealthMetricSyncItem['metricType'];
  value: number;
  minimumValue: number;
  maximumValue: number;
  count: number;
  unit: string;
  aggregation: 'AVERAGE' | 'SUM';
};

export type HealthSourceSummary = {
  sourceType: string;
  syncEnabled: boolean;
  revokedAt: string | null;
  recordCount: number;
  oldestRecordAt: string | null;
  newestRecordAt: string | null;
  recordsByMetricType: Record<string, number>;
};

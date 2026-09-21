export type HealthMetricType = "STEP_COUNT" | "HEART_RATE" | "SPO2" | "SLEEP_SESSION" | "EXERCISE_SESSION";

export interface HealthMetricRecord {
  id: string;
  metricType: HealthMetricType;
  value: number | null;
  unit: string | null;
  sourceType: string;
  sourceName: string | null;
  dataOrigin: string | null;
  recordedAt: string;
  startTime: string | null;
  endTime: string | null;
  sourceLastModifiedAt: string | null;
  details: Record<string, unknown>;
  createdAt: string;
}

export interface HealthTrendPoint {
  periodStart: string;
  periodEnd: string;
  metricType: HealthMetricType;
  value: number;
  minimumValue: number;
  maximumValue: number;
  count: number;
  unit: string;
  aggregation: "AVERAGE" | "SUM";
}

import { httpClient, type CursorPage } from "@/shared";
import type { HealthMetricRecord, HealthMetricType, HealthTrendPoint } from "../types/health.types";

export interface HealthSourceSummary {
  sourceType: string;
  syncEnabled: boolean;
  revokedAt: string | null;
  recordCount: number;
  oldestRecordAt: string | null;
  newestRecordAt: string | null;
  recordsByMetricType: Record<string, number>;
}

const timezone = "Asia/Ho_Chi_Minh";

export const healthApi = {
  async trends(metricType: HealthMetricType, from: string, to: string): Promise<HealthTrendPoint[]> {
    const { data } = await httpClient.get<HealthTrendPoint[]>("/api/v1/health-metrics/trends", {
      params: { metricType, from, to, bucket: "DAY", timezone },
    });
    return data;
  },

  async history(metricType: HealthMetricType, from: string, to: string, limit = 50): Promise<CursorPage<HealthMetricRecord>> {
    const { data } = await httpClient.get<CursorPage<HealthMetricRecord>>("/api/v1/health-metrics", {
      params: { metricType, from, to, limit },
    });
    return data;
  },
  async sourceSummary(): Promise<HealthSourceSummary> {
    const { data } = await httpClient.get<HealthSourceSummary>("/api/v1/health-metrics/sources/HEALTH_CONNECT");
    return data;
  },
  async deleteHealthConnectData(): Promise<HealthSourceSummary> {
    const { data } = await httpClient.delete<HealthSourceSummary>("/api/v1/health-metrics/sources/HEALTH_CONNECT/data");
    return data;
  },
};

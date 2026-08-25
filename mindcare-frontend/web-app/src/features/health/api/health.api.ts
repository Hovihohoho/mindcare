import { httpClient, type CursorPage } from "@/shared";
import type { HealthMetricRecord, HealthMetricType, HealthTrendPoint } from "../types/health.types";

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
};

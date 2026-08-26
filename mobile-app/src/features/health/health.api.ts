import { apiRequest } from '@/services/api/api.client';
import type { HealthMetricBatchResponse, HealthMetricSyncItem, HealthSourceSummary, HealthTrendPoint } from './health.types';

export const healthApi = {
  syncMetrics(token: string, items: HealthMetricSyncItem[], idempotencyKey: string) {
    return apiRequest<HealthMetricBatchResponse>('/api/v1/health-metrics/sync', {
      method: 'POST',
      token,
      responseType: 'raw',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: { sourceType: 'HEALTH_CONNECT', items },
    });
  },
  trends(token: string, metricType: HealthMetricSyncItem['metricType'], from: string, to: string) {
    const query = new URLSearchParams({ metricType, from, to, bucket: 'DAY', timezone: 'Asia/Ho_Chi_Minh' });
    return apiRequest<HealthTrendPoint[]>(`/api/v1/health-metrics/trends?${query}`, {
      token,
      responseType: 'raw',
    });
  },
  enableSource(token: string) {
    return apiRequest<HealthSourceSummary>('/api/v1/health-metrics/sources/HEALTH_CONNECT/enable', {
      method: 'POST', token, responseType: 'raw',
    });
  },
  deleteSourceData(token: string) {
    return apiRequest<HealthSourceSummary>('/api/v1/health-metrics/sources/HEALTH_CONNECT/data', {
      method: 'DELETE', token, responseType: 'raw',
    });
  },
};

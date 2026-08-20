import { apiRequest } from '@/services/api/api.client';
import type { HealthMetricBatchResponse, HealthMetricSyncItem } from './health.types';

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
};

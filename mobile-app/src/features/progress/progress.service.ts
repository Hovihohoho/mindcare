import { assessmentService, type AssessmentResult } from '@/services/assessment/assessment.service';
import { apiRequest } from '@/services/api/api.client';
import { emotionService } from '@/services/emotion/emotion.service';
import type { EmotionTrendPoint } from '@/features/emotion/emotion.types';
import { healthApi } from '@/features/health/health.api';
import type { HealthMetricSyncItem, HealthTrendPoint } from '@/features/health/health.types';

export type CarePlanSummary = { completedThisWeek: number; targetThisWeek: number };
export type ProgressData = {
  currentCheckIns: number;
  previousCheckIns: number;
  emotionTrends: EmotionTrendPoint[];
  currentHealth: Record<string, HealthTrendPoint[]>;
  previousHealth: Record<string, HealthTrendPoint[]>;
  latestAssessment: AssessmentResult | null;
  carePlan: CarePlanSummary | null;
};

const metrics: HealthMetricSyncItem['metricType'][] = ['STEP_COUNT', 'SLEEP_SESSION', 'EXERCISE_SESSION'];

function range(daysAgo: number, length: number) {
  const to = new Date(); to.setDate(to.getDate() - daysAgo);
  const from = new Date(to); from.setDate(from.getDate() - length);
  return { from: from.toISOString(), to: to.toISOString() };
}

async function optionalPlan(token: string) {
  try { return await apiRequest<CarePlanSummary>('/api/v1/self-care-plan', { token, responseType: 'raw' }); }
  catch { return null; }
}

export const progressService = {
  async get(token: string): Promise<ProgressData> {
    const current = range(0, 7), previous = range(7, 7), history = range(0, 90);
    const [currentEmotion, previousEmotion, currentHealth, previousHealth, assessments, carePlan] = await Promise.all([
      emotionService.trends(token, current.from, current.to), emotionService.trends(token, previous.from, previous.to),
      Promise.all(metrics.map(async (metric) => [metric, await healthApi.trends(token, metric, current.from, current.to)] as const)),
      Promise.all(metrics.map(async (metric) => [metric, await healthApi.trends(token, metric, previous.from, previous.to)] as const)),
      assessmentService.history(token, history.from, history.to), optionalPlan(token),
    ]);
    return {
      currentCheckIns: currentEmotion.reduce((sum, point) => sum + point.count, 0),
      previousCheckIns: previousEmotion.reduce((sum, point) => sum + point.count, 0),
      emotionTrends: currentEmotion,
      currentHealth: Object.fromEntries(currentHealth), previousHealth: Object.fromEntries(previousHealth),
      latestAssessment: assessments.items[0] ?? null, carePlan,
    };
  },
};

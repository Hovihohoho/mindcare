import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { carePlanApi, type CarePlan } from "@/features/care-plan/api/carePlan.api";
import { emotionApi } from "@/features/emotion/api/emotion.api";
import { healthApi } from "@/features/health/api/health.api";
import type { HealthMetricType } from "@/features/health/types/health.types";

const metricTypes: HealthMetricType[] = ["STEP_COUNT", "SLEEP_SESSION", "EXERCISE_SESSION"];

function range(daysAgo: number, length: number) {
  const to = new Date();
  to.setDate(to.getDate() - daysAgo);
  const from = new Date(to);
  from.setDate(from.getDate() - length);
  return { from: from.toISOString(), to: to.toISOString() };
}

async function optionalCarePlan(): Promise<CarePlan | null> {
  try { return await carePlanApi.get(); } catch { return null; }
}

export const progressApi = {
  async get() {
    const current = range(0, 7);
    const previous = range(7, 7);
    const assessmentRange = range(0, 90);
    const [currentEmotion, previousEmotion, currentHealth, previousHealth, assessments, carePlan] = await Promise.all([
      emotionApi.trends(current.from, current.to), emotionApi.trends(previous.from, previous.to),
      Promise.all(metricTypes.map(async (type) => [type, await healthApi.trends(type, current.from, current.to)] as const)),
      Promise.all(metricTypes.map(async (type) => [type, await healthApi.trends(type, previous.from, previous.to)] as const)),
      assessmentApi.history(assessmentRange.from, assessmentRange.to), optionalCarePlan(),
    ]);
    return { currentEmotion, previousEmotion, currentHealth: Object.fromEntries(currentHealth), previousHealth: Object.fromEntries(previousHealth), latestAssessment: assessments.items[0] ?? null, carePlan };
  },
};

import { httpClient, type CursorPage } from "@/shared";
import { assessmentMocks, gadQuestions } from "../constants/assessment.mock";
import type { Assessment, AssessmentResult } from "../types/assessment.types";

const fallbackEnabled = import.meta.env.VITE_ENABLE_API_MOCK_FALLBACK !== "false";

export const assessmentApi = {
  async list(): Promise<Assessment[]> {
    try {
      const { data } = await httpClient.get<Assessment[]>("/api/v1/assessments");
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      return assessmentMocks;
    }
  },
  async detail(code: string): Promise<Assessment> {
    try {
      const { data } = await httpClient.get<Assessment>(`/api/v1/assessments/${code}`);
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      return { ...(assessmentMocks.find((item) => item.code === code) ?? assessmentMocks[0]), questions: gadQuestions };
    }
  },
  async submit(code: string, answers: { questionId: string; optionId: string }[]): Promise<AssessmentResult> {
    const { data } = await httpClient.post<AssessmentResult>(`/api/v1/assessments/${code}/submissions`, { answers }, { headers: { "Idempotency-Key": crypto.randomUUID() } });
    return data;
  },
  async history(from: string, to: string) {
    const { data } = await httpClient.get<CursorPage<AssessmentResult>>("/api/v1/assessment-results", { params: { from, to } });
    return data;
  },
};

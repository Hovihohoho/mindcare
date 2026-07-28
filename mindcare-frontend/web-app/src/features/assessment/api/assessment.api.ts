import { httpClient, type CursorPage } from "@/shared";
import type { Assessment, AssessmentResult } from "../types/assessment.types";

export const assessmentApi = {
  async list(): Promise<Assessment[]> {
    const { data } = await httpClient.get<Assessment[]>("/api/v1/assessments");
    return data;
  },
  async detail(code: string): Promise<Assessment> {
    const { data } = await httpClient.get<Assessment>(`/api/v1/assessments/${code}`);
    return data;
  },
  async submit(code: string, assessmentVersion: number, answers: { questionId: string; optionId: string }[]): Promise<AssessmentResult> {
    const { data } = await httpClient.post<AssessmentResult>(
      `/api/v1/assessments/${code}/submissions`,
      { assessmentVersion, answers },
      { headers: { "Idempotency-Key": crypto.randomUUID() } },
    );
    return data;
  },
  async result(resultId: string): Promise<AssessmentResult> {
    const { data } = await httpClient.get<AssessmentResult>(`/api/v1/assessment-results/${resultId}`);
    return data;
  },
  async history(from: string, to: string) {
    const { data } = await httpClient.get<CursorPage<AssessmentResult>>("/api/v1/assessment-results", { params: { from, to } });
    return data;
  },
};

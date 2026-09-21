import { apiRequest } from '@/services/api/api.client';

export type AssessmentSummary = {
  id: string;
  code: string;
  assessmentVersion: number;
  title: string;
  description: string;
  evidence: AssessmentEvidence | null;
};

export type AssessmentEvidence = {
  publisher: string;
  sourceTitle: string;
  sourceUrl: string;
  publicationYear: number;
  instrumentVersion: string;
  license: string;
  scoringRuleVersion: string;
  purpose: string;
  limitation: string;
};

export type AssessmentQuestion = {
  id: string;
  questionText: string;
  orderIndex: number;
  answerOptions: { id: string; optionText: string }[];
};

export type AssessmentDetail = AssessmentSummary & { questions: AssessmentQuestion[] };

export type AssessmentResult = {
  resultId: string;
  assessmentCode: string;
  assessmentVersion: number;
  totalScore: number;
  riskLevel: 'NORMAL' | 'MILD' | 'MODERATE' | 'SEVERE' | 'EXTREME';
  screeningNotice: string;
  recommendations: string[];
  createdAt: string;
};

type CursorPage<T> = { items: T[]; nextCursor: string | null; hasMore: boolean };

function query(params: Record<string, string | number>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => search.set(key, String(value)));
  return search.toString();
}

export const assessmentService = {
  list(token: string) {
    return apiRequest<AssessmentSummary[]>('/api/v1/assessments', { token, responseType: 'raw' });
  },
  detail(token: string, code: string) {
    return apiRequest<AssessmentDetail>(`/api/v1/assessments/${encodeURIComponent(code)}`, { token, responseType: 'raw' });
  },
  history(token: string, from: string, to: string) {
    const params = query({ from, to, limit: 100 });
    return apiRequest<CursorPage<AssessmentResult>>(`/api/v1/assessment-results?${params}`, { token, responseType: 'raw' });
  },
  submit(token: string, code: string, assessmentVersion: number, answers: { questionId: string; optionId: string }[]) {
    return apiRequest<AssessmentResult>(`/api/v1/assessments/${encodeURIComponent(code)}/submissions`, {
      body: { assessmentVersion, answers },
      headers: { 'Idempotency-Key': createIdempotencyKey() },
      method: 'POST',
      responseType: 'raw',
      token,
    });
  },
};

function createIdempotencyKey() {
  const cryptoApi = globalThis.crypto as Crypto | undefined;
  return cryptoApi?.randomUUID?.() ?? `mobile-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

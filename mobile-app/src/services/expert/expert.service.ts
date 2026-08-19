import { apiRequest } from '@/services/api/api.client';

export type ExpertSummary = {
  expertUserId: string;
  displayName: string;
  headline: string | null;
  specialties: string[];
  yearsOfExperience: number;
  consultationCount: number;
  avatarUrl?: string | null;
  bio?: string | null;
  location?: string | null;
  education?: string | null;
};

type ExpertPage = { items: ExpertSummary[]; nextCursor: string | null; hasMore: boolean };

export const expertService = {
  list(token: string) {
    return apiRequest<ExpertPage>('/api/v1/experts?limit=100', { token, responseType: 'raw' });
  },
};

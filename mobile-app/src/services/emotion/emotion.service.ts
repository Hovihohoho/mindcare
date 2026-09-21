import { apiRequest } from '@/services/api/api.client';
import type { CursorPage, EmotionJournal, EmotionLevel, EmotionTrendPoint } from '@/features/emotion/emotion.types';

function query(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) search.set(key, String(value));
  });
  return search.toString();
}

export const emotionService = {
  create(token: string, payload: { emotionType: EmotionLevel; content: string }) {
    return apiRequest<EmotionJournal>('/api/v1/emotion-journals', { method: 'POST', body: payload, token, responseType: 'raw' });
  },

  history(token: string, from: string, to: string, limit = 30, cursor?: string) {
    const params = query({ from, to, limit, cursor });
    return apiRequest<CursorPage<EmotionJournal>>(`/api/v1/emotion-journals?${params}`, { token, responseType: 'raw' });
  },

  trends(token: string, from: string, to: string) {
    const params = query({ from, to, bucket: 'DAY', timezone: 'Asia/Ho_Chi_Minh' });
    return apiRequest<EmotionTrendPoint[]>(`/api/v1/emotion-trends?${params}`, { token, responseType: 'raw' });
  },
};

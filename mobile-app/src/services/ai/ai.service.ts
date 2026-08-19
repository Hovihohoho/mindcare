import { apiRequest } from '@/services/api/api.client';

export type ChatSource = { id: string; title: string; sourceUrl: string; similarity: number };
export type AiAnswer = { answer: string; sources: ChatSource[] };

export const aiService = {
  ask(token: string, question: string) {
    return apiRequest<AiAnswer>('/api/ai/chat', {
      body: { question, topK: 5 },
      method: 'POST',
      token,
    });
  },
};

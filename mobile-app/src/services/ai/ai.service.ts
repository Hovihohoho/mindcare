import { apiRequest } from '@/services/api/api.client';
import { createRequestId } from '@/services/api/retry-submission';

export type ChatSource = { id: string; title: string; sourceUrl: string; similarity: number };
export type ChatSafetyDirective = {
  level: 'NONE' | 'CHECK_IN' | 'EXPLICIT' | 'IMMINENT';
  showSafetyCheck: boolean;
  showEmergencyActions: boolean;
  emergencyNumber?: string | null;
};
export type AiAnswer = { answer: string; sources: ChatSource[]; safety: ChatSafetyDirective; conversationId: string };
export type ConversationSummary = { id: string; title: string; createdAt: string; updatedAt: string };
export type ConversationDetail = ConversationSummary & {
  messages: { id: string; role: 'user' | 'assistant'; content: string; sources: ChatSource[]; safetyLevel: ChatSafetyDirective['level']; createdAt: string }[];
};

export const aiService = {
  ask(token: string, question: string, conversationId?: string, requestId = createRequestId()) {
    return apiRequest<AiAnswer>('/api/ai/chat', {
      body: { question, topK: 5, conversationId, requestId },
      method: 'POST',
      timeout: 120_000,
      token,
    });
  },
  listConversations(token: string) {
    return apiRequest<ConversationSummary[]>('/api/ai/chat/conversations', { token });
  },
  getConversation(token: string, id: string) {
    return apiRequest<ConversationDetail>(`/api/ai/chat/conversations/${id}`, { token });
  },
  renameConversation(token: string, id: string, title: string) {
    return apiRequest<ConversationSummary>(`/api/ai/chat/conversations/${id}`, { body: { title }, method: 'PATCH', token });
  },
  deleteConversation(token: string, id: string) {
    return apiRequest<void>(`/api/ai/chat/conversations/${id}`, { method: 'DELETE', token });
  },
};

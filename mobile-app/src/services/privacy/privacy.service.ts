import { apiRequest } from '@/services/api/api.client';

export const privacyService = {
  exportEmotionData: (token: string) => apiRequest<Record<string, unknown>>('/api/v1/privacy/export', { responseType: 'raw', token }),
  exportAiData: (token: string) => apiRequest<Record<string, unknown>>('/api/ai/privacy/export', { token }),
  deleteEmotionData: (token: string) => apiRequest<Record<string, number>>('/api/v1/privacy/data', { method: 'DELETE', responseType: 'raw', token }),
  deleteAiData: (token: string) => apiRequest<{ deletedConversations: number }>('/api/ai/privacy/data', { method: 'DELETE', token }),
};

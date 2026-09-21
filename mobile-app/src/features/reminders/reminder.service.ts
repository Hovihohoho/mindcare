import { apiRequest } from '@/services/api/api.client';
export type ReminderType = 'DAILY_CHECK_IN' | 'SELF_CARE';
export type Reminder = { id: string; reminderType: ReminderType; enabled: boolean; localTime: string; timezone: string };
export const reminderService = { list: (token: string) => apiRequest<Reminder[]>('/api/v1/reminders', { token }), update: (token: string, item: Reminder) => apiRequest<Reminder>(`/api/v1/reminders/${item.reminderType}`, { method: 'PUT', token, body: { enabled: item.enabled, localTime: item.localTime, timezone: item.timezone } }) };

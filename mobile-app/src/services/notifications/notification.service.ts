import { apiRequest } from '@/services/api/api.client';

export type NotificationItem = {
  id: string; type: string; title: string; message: string;
  actionUrl: string | null; readAt: string | null; createdAt: string;
};
export type NotificationPage = {
  items: NotificationItem[]; page: number; size: number;
  totalElements: number; totalPages: number; unreadCount: number;
};
export function getNotifications(token: string, page = 0, size = 20) {
  return apiRequest<NotificationPage>(`/api/v1/notifications?page=${page}&size=${size}`, { token });
}

export function getUnreadCount(token: string) {
  return apiRequest<number>('/api/auth/notifications/unread-count', { token });
}

export function markNotificationRead(token: string, id: string) {
  return apiRequest<void>(`/api/v1/notifications/${encodeURIComponent(id)}/read`, { token, method: 'PATCH' });
}

export function markAllNotificationsRead(token: string) {
  return apiRequest<void>('/api/v1/notifications/read-all', { token, method: 'PATCH' });
}

export const notificationService = {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
};

import { httpClient, type ApiResponse } from "@/shared";

export interface NotificationItem {
  id: string;
  title: string;
  content: string;
  notificationType: string;
  actionUrl?: string;
  readAt?: string;
  createdAt: string;
}

export const notificationApi = {
  async list() {
    const { data } = await httpClient.get<ApiResponse<NotificationItem[]>>("/api/auth/notifications");
    return data.data;
  },
  async unreadCount() {
    const { data } = await httpClient.get<ApiResponse<number>>("/api/auth/notifications/unread-count");
    return data.data;
  },
  async markRead(id: string) {
    await httpClient.patch(`/api/auth/notifications/${id}/read`);
  },
  async markAllRead() {
    await httpClient.patch("/api/auth/notifications/read-all");
  },
};

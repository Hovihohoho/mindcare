import { httpClient, type ApiResponse } from "@/shared";
import type { NotificationItem } from "../types/notification.types";

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
    const { data } = await httpClient.patch<ApiResponse<NotificationItem>>(`/api/auth/notifications/${id}/read`);
    return data.data;
  },
};

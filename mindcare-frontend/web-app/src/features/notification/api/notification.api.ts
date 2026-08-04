import { httpClient, type ApiResponse } from "@/shared";
import type { NotificationPage } from "../types/notification.types";

export const notificationApi = {
  async list(page = 0, size = 8) {
    const { data } = await httpClient.get<ApiResponse<NotificationPage>>("/api/v1/notifications", { params: { page, size } });
    return data.data;
  },
  async markRead(id: string) {
    await httpClient.patch(`/api/v1/notifications/${id}/read`);
  },
  async markAllRead() {
    await httpClient.patch("/api/v1/notifications/read-all");
  },
};

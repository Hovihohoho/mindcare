import { httpClient } from "@/shared";

export interface ExpertClientProfile {
  userId: string;
  fullName: string;
  email?: string | null;
  createdAt?: string | null;
  bookingId: string;
  scheduleId: string;
  bookingStatus: string;
}

export const clientRecordApi = {
  async bySchedule(scheduleId: string): Promise<ExpertClientProfile> {
    const { data } = await httpClient.get<ExpertClientProfile>(
      `/api/v1/expert/clients/by-schedule/${scheduleId}`,
    );
    return data;
  },

  async byUser(userId: string): Promise<ExpertClientProfile> {
    const { data } = await httpClient.get<ExpertClientProfile>(
      `/api/v1/expert/clients/${userId}`,
    );
    return data;
  },
};

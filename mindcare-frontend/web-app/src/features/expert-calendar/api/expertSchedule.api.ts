import { httpClient } from "@/shared";

export interface ExpertSchedule {
  id: string;
  expertUserId: string;
  startAt: string;
  endAt: string;
  status: "AVAILABLE" | "HELD" | "BOOKED" | "CANCELLED";
}

export const expertScheduleApi = {
  async list(from: string, to: string): Promise<ExpertSchedule[]> {
    const { data } = await httpClient.get<ExpertSchedule[]>("/api/v1/expert/schedules", {
      params: { from, to, limit: 100 },
    });
    return data;
  },
  async create(startAt: string, endAt: string): Promise<ExpertSchedule> {
    const { data } = await httpClient.post<ExpertSchedule>("/api/v1/expert/schedules", { startAt, endAt });
    return data;
  },
  async update(scheduleId: string, startAt: string, endAt: string): Promise<ExpertSchedule> {
    const { data } = await httpClient.put<ExpertSchedule>(`/api/v1/expert/schedules/${scheduleId}`, { startAt, endAt });
    return data;
  },
  async remove(scheduleId: string): Promise<void> {
    await httpClient.delete(`/api/v1/expert/schedules/${scheduleId}`);
  },
};

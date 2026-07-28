import { addDays, startOfDay } from "date-fns";
import { httpClient, type CursorPage } from "@/shared";
import type { ExpertBooking, ExpertSchedule } from "../types/expert-workspace.types";

export const expertWorkspaceApi = {
  async schedules(): Promise<ExpertSchedule[]> {
    const from = startOfDay(new Date()).toISOString();
    const to = addDays(new Date(), 90).toISOString();
    const { data } = await httpClient.get<ExpertSchedule[]>(
      "/api/v1/expert/schedules",
      { params: { from, to, limit: 100 } },
    );
    return data;
  },

  async createSchedule(startAt: string, endAt: string): Promise<ExpertSchedule> {
    const { data } = await httpClient.post<ExpertSchedule>(
      "/api/v1/expert/schedules",
      { startAt, endAt },
    );
    return data;
  },

  async cancelSchedule(scheduleId: string): Promise<void> {
    await httpClient.delete(`/api/v1/expert/schedules/${scheduleId}`);
  },

  async bookings(limit = 100): Promise<CursorPage<ExpertBooking>> {
    const { data } = await httpClient.get<CursorPage<ExpertBooking>>(
      "/api/v1/expert/bookings",
      { params: { limit } },
    );
    return data;
  },
};

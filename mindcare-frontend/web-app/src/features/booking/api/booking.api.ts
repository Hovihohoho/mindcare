import { addDays, setHours } from "date-fns";
import { httpClient } from "@/shared";
import type { BookingCheckout, ExpertSchedule } from "../types/booking.types";

const fallbackEnabled = import.meta.env.VITE_ENABLE_API_MOCK_FALLBACK !== "false";

function mockSchedules(expertUserId: string): ExpertSchedule[] {
  return Array.from({ length: 12 }, (_, index) => {
    const start = setHours(addDays(new Date(), Math.floor(index / 3) + 1), 9 + (index % 3) * 2);
    return { id: `schedule-${index + 1}`, expertUserId, startAt: start.toISOString(), endAt: new Date(start.getTime() + 60 * 60 * 1000).toISOString(), status: index === 4 ? "BOOKED" : "AVAILABLE" };
  });
}

export const bookingApi = {
  async schedules(expertUserId: string): Promise<ExpertSchedule[]> {
    const from = new Date().toISOString();
    const to = addDays(new Date(), 30).toISOString();
    try {
      const { data } = await httpClient.get<ExpertSchedule[]>(`/api/v1/experts/${expertUserId}/schedules`, { params: { from, to } });
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      return mockSchedules(expertUserId);
    }
  },
  async create(scheduleId: string, note: string): Promise<BookingCheckout> {
    const { data } = await httpClient.post<BookingCheckout>("/api/v1/bookings", { scheduleId, note, paymentMethod: "VNPAY" });
    return data;
  },
};

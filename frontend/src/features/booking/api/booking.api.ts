import { addDays } from "date-fns";
import { httpClient } from "@/shared";
import type { BookingCheckout, ExpertSchedule } from "../types/booking.types";

export const bookingApi = {
  async schedules(expertUserId: string): Promise<ExpertSchedule[]> {
    const from = new Date().toISOString();
    const to = addDays(new Date(), 30).toISOString();
    const { data } = await httpClient.get<ExpertSchedule[]>(`/api/v1/experts/${expertUserId}/schedules`, { params: { from, to, limit: 100 } });
    return data;
  },
  async create(scheduleId: string, note: string): Promise<BookingCheckout> {
    const { data } = await httpClient.post<BookingCheckout>(
      "/api/v1/bookings",
      { scheduleId, note, paymentMethod: "VNPAY" },
      { headers: { "Idempotency-Key": crypto.randomUUID() } },
    );
    return data;
  },
};

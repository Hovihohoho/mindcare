import { addDays } from "date-fns";
import { httpClient, type CursorPage } from "@/shared";
import type { Booking, BookingCheckout, ExpertSchedule } from "../types/booking.types";

interface BookingHistoryParams {
  status?: string;
  cursor?: string;
  limit?: number;
}

export const bookingApi = {
  async schedules(expertUserId: string): Promise<ExpertSchedule[]> {
    const from = new Date().toISOString();
    const to = addDays(new Date(), 30).toISOString();
    const { data } = await httpClient.get<ExpertSchedule[]>(`/api/v1/experts/${expertUserId}/schedules`, { params: { from, to } });
    return data;
  },
  async create(scheduleId: string, note: string): Promise<BookingCheckout> {
    const { data } = await httpClient.post<BookingCheckout>(
      "/api/v1/bookings",
      { scheduleId, note, paymentMethod: null },
      { headers: { "Idempotency-Key": crypto.randomUUID() } },
    );
    return data;
  },
  async history(params: BookingHistoryParams = {}): Promise<CursorPage<Booking>> {
    const { data } = await httpClient.get<CursorPage<Booking>>("/api/v1/bookings", {
      params: { limit: params.limit ?? 20, status: params.status, cursor: params.cursor },
    });
    return data;
  },
  async detail(bookingId: string, role?: string): Promise<Booking> {
    const path = role === "ROLE_EXPERT"
      ? `/api/v1/expert/bookings/${bookingId}`
      : `/api/v1/bookings/${bookingId}`;
    const { data } = await httpClient.get<Booking>(path);
    return data;
  },
};

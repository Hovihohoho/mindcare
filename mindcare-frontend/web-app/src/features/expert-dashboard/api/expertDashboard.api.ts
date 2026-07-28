import { httpClient, type CursorPage } from "@/shared";

export interface ExpertBooking {
  id: string;
  userId: string;
  expertUserId: string;
  scheduleId: string;
  status: string;
  price: number;
  currency: string;
  paymentStatus: string;
  createdAt: string;
  confirmedAt?: string | null;
}

export async function getExpertBookings(): Promise<CursorPage<ExpertBooking>> {
  const { data } = await httpClient.get<CursorPage<ExpertBooking>>("/api/v1/expert/bookings", {
    params: { limit: 100 },
  });
  return data;
}

export type ScheduleStatus = "AVAILABLE" | "HELD" | "BOOKED" | "CANCELED";

export interface ExpertSchedule {
  id: string;
  expertUserId: string;
  startAt: string;
  endAt: string;
  status: ScheduleStatus;
  holdExpiresAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExpertBooking {
  id: string;
  userId: string;
  expertUserId: string;
  scheduleId: string;
  status: string;
  note?: string | null;
  price: number;
  currency: string;
  paymentStatus: string;
  createdAt: string;
  updatedAt: string;
}

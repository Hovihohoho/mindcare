export interface ExpertSchedule {
  id: string;
  expertUserId: string;
  startAt: string;
  endAt: string;
  status: "AVAILABLE" | "HELD" | "BOOKED" | "CANCELLED";
}

export interface Booking {
  id: string;
  userId: string;
  expertUserId: string;
  scheduleId: string;
  status: string;
  note?: string | null;
  price: number;
  currency: string;
  paymentStatus: string;
  cancellationReason?: string | null;
  expiresAt?: string | null;
  confirmedAt?: string | null;
  cancellationRequestedAt?: string | null;
  cancellationReviewDeadline?: string | null;
  cancellationDecisionReason?: string | null;
  completedAt?: string | null;
  canceledAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface BookingCheckout {
  booking: Booking;
  payment?: { id: string; checkoutUrl?: string; status: string };
  paymentRequired: boolean;
}

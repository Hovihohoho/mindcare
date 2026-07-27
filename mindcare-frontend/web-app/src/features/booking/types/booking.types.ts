export interface ExpertSchedule {
  id: string;
  expertUserId: string;
  startAt: string;
  endAt: string;
  status: "AVAILABLE" | "HELD" | "BOOKED" | "CANCELED";
}

export interface Booking {
  id: string;
  expertUserId: string;
  scheduleId: string;
  status: string;
  price: number;
  currency: string;
  paymentStatus: string;
}

export interface BookingCheckout {
  booking: Booking;
  payment?: { id: string; checkoutUrl?: string; status: string };
  paymentRequired: boolean;
}

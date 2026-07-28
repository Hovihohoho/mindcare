import { ExternalLink, ShieldCheck } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { Button, Card, PageHeader } from "@/shared";
import type { BookingCheckout, ExpertSchedule } from "@/features/booking";

export function PaymentPage() {
  const state = useLocation().state as { checkout?: BookingCheckout; selected?: ExpertSchedule } | null;
  const checkout = state?.checkout;

  if (!checkout) {
    return <Card className="mx-auto max-w-xl p-8 text-center"><h1 className="text-2xl font-extrabold">Không có phiên đặt lịch</h1><p className="mt-3 text-muted">Hãy chọn chuyên gia và khung giờ trước.</p><Link to="/experts"><Button className="mt-5">Tìm chuyên gia</Button></Link></Card>;
  }

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <PageHeader title={checkout.paymentRequired ? "Thanh toán booking" : "Đặt lịch thành công"} description={`Mã booking: ${checkout.booking.id}`} />
      <Card className="p-8">
        <dl className="grid gap-4 sm:grid-cols-2">
          <div><dt className="text-sm text-muted">Trạng thái booking</dt><dd className="mt-1 font-bold">{checkout.booking.status}</dd></div>
          <div><dt className="text-sm text-muted">Trạng thái thanh toán</dt><dd className="mt-1 font-bold">{checkout.booking.paymentStatus}</dd></div>
          <div><dt className="text-sm text-muted">Thời gian</dt><dd className="mt-1 font-bold">{state?.selected ? new Date(state.selected.startAt).toLocaleString("vi-VN") : "—"}</dd></div>
          <div><dt className="text-sm text-muted">Tổng tiền</dt><dd className="mt-1 font-bold">{Number(checkout.booking.price).toLocaleString("vi-VN")} {checkout.booking.currency}</dd></div>
        </dl>
        {checkout.paymentRequired && checkout.payment?.checkoutUrl ? (
          <a href={checkout.payment.checkoutUrl} rel="noreferrer"><Button className="mt-7 w-full" size="lg">Mở cổng thanh toán <ExternalLink className="size-4" /></Button></a>
        ) : <Link to="/"><Button className="mt-7 w-full" size="lg">Về trang chủ</Button></Link>}
        <p className="mt-6 flex gap-2 text-xs text-muted"><ShieldCheck className="size-4 shrink-0" />Không giả lập thanh toán ở frontend. Trạng thái được trả từ Booking Service.</p>
      </Card>
    </div>
  );
}

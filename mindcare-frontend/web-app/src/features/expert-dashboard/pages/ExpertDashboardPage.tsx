import { useQuery } from "@tanstack/react-query";
import { CalendarCheck, ClipboardPlus, MessageSquareText, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Loading } from "@/shared";
import { getExpertBookings } from "../api/expertDashboard.api";
import { ExpertMetricCard } from "../components/ExpertMetricCard";

export function ExpertDashboardPage() {
  const bookings = useQuery({ queryKey: ["expert-bookings"], queryFn: getExpertBookings });
  if (bookings.isLoading) return <Loading />;

  const items = bookings.data?.items ?? [];
  const today = new Date().toDateString();
  const todayItems = items.filter((item) => item.confirmedAt && new Date(item.confirmedAt).toDateString() === today);
  const pendingItems = items.filter((item) => item.status === "PAYMENT_PENDING" || item.status === "CANCELLATION_PENDING");

  return (
    <div className="mx-auto max-w-[1180px] space-y-8">
      <header>
        <h1 className="text-3xl font-bold text-[#075da5]">Bảng điều khiển chuyên gia</h1>
        <p className="mt-2 text-sm text-slate-600">Dữ liệu lịch hẹn được tải trực tiếp từ Booking Service.</p>
      </header>
      <div className="grid gap-6 md:grid-cols-3">
        <ExpertMetricCard label="Lịch hôm nay" value={String(todayItems.length)} hint="Thực tế" icon={<CalendarCheck />} tone="bg-blue-100 text-blue-600" />
        <ExpertMetricCard label="Yêu cầu đang chờ" value={String(pendingItems.length)} hint="Thực tế" icon={<ClipboardPlus />} tone="bg-emerald-200 text-emerald-700" />
        <ExpertMetricCard label="Booking đã tải" value={String(items.length)} hint={bookings.data?.hasMore ? "Còn dữ liệu" : "Trang hiện tại"} icon={<UsersRound />} tone="bg-blue-100 text-blue-700" />
      </div>
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Danh sách lịch hẹn</h2>
        <div className="mt-5 space-y-4">
          {items.map((item) => (
            <article className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-line p-4" key={item.id}>
              <div>
                <h3 className="font-bold">Khách hàng {item.userId.slice(0, 8)}</h3>
                <p className="mt-1 text-sm text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</p>
              </div>
              <div className="text-right">
                <Badge tone={item.status === "CONFIRMED" ? "success" : "neutral"}>{item.status}</Badge>
                <p className="mt-2 text-sm font-semibold">{Number(item.price).toLocaleString("vi-VN")} {item.currency}</p>
                <Link className="mt-3 inline-block" to={`/expert/chat/${item.id}`}>
                  <Button size="sm" variant="outline" leftIcon={<MessageSquareText className="size-4" />}>Chat</Button>
                </Link>
              </div>
            </article>
          ))}
          {items.length === 0 && <p className="py-10 text-center text-muted">Chưa có booking nào.</p>}
        </div>
      </Card>
    </div>
  );
}

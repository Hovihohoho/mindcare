import { useQuery } from "@tanstack/react-query";
import { CalendarCheck, CalendarDays, ClipboardPlus, UsersRound } from "lucide-react";
import { format, isToday } from "date-fns";
import { vi } from "date-fns/locale";
import { Card, EmptyState, Loading } from "@/shared";
import type { AuthUser } from "@/features/auth";
import { expertWorkspaceApi } from "@/features/expert-calendar";
import { userStorage } from "@/shared/lib/storage";
import { ExpertMetricCard } from "../components/ExpertMetricCard";

export function ExpertDashboardPage() {
  const user = userStorage.get<AuthUser>();
  const schedules = useQuery({ queryKey: ["expert-schedules"], queryFn: expertWorkspaceApi.schedules });
  const bookings = useQuery({ queryKey: ["expert-bookings"], queryFn: () => expertWorkspaceApi.bookings() });
  const items = bookings.data?.items ?? [];
  const todayCount = schedules.data?.filter((item) => isToday(new Date(item.startAt)) && item.status === "BOOKED").length ?? 0;
  const waitingCount = items.filter((item) => ["PENDING_PAYMENT", "PENDING_CONFIRMATION"].includes(item.status)).length;
  const completedCount = items.filter((item) => item.status === "COMPLETED").length;

  return (
    <div className="mx-auto max-w-[1180px] space-y-8">
      <header>
        <h1 className="text-3xl font-bold text-brand-700">Xin chào, {user?.fullName ?? "chuyên gia"}</h1>
        <p className="mt-2 flex items-center gap-2 text-sm text-slate-600">
          <CalendarDays className="size-4 text-blue-600" />
          {format(new Date(), "EEEE, dd MMMM yyyy", { locale: vi })}
        </p>
      </header>
      <div className="grid gap-6 md:grid-cols-3">
        <ExpertMetricCard label="Lịch hôm nay" value={String(todayCount)} hint="Theo lịch hiện tại" icon={<CalendarCheck />} tone="bg-blue-100 text-blue-600" />
        <ExpertMetricCard label="Yêu cầu đang chờ" value={String(waitingCount)} hint="Cần theo dõi" icon={<ClipboardPlus />} tone="bg-emerald-200 text-emerald-700" />
        <ExpertMetricCard label="Ca đã hoàn thành" value={String(completedCount)} hint="Trong dữ liệu đã tải" icon={<UsersRound />} tone="bg-blue-100 text-blue-700" />
      </div>
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Lịch hẹn gần đây</h2>
        {bookings.isLoading ? <Loading /> : bookings.isError ? (
          <p className="mt-5 text-sm text-rose-600">Không thể tải lịch hẹn.</p>
        ) : items.length ? (
          <div className="mt-5 divide-y divide-line">
            {items.slice(0, 8).map((item) => (
              <div className="flex flex-wrap items-center justify-between gap-3 py-4" key={item.id}>
                <div><b>Mã lịch hẹn #{item.id.slice(0, 8)}</b><p className="mt-1 text-sm text-muted">Tạo lúc {format(new Date(item.createdAt), "dd/MM/yyyy HH:mm")}</p></div>
                <div className="text-right"><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold">{item.status}</span><p className="mt-2 text-sm">{item.price.toLocaleString("vi-VN")} {item.currency}</p></div>
              </div>
            ))}
          </div>
        ) : <div className="mt-5"><EmptyState title="Chưa có lịch hẹn" description="Các lịch hẹn mới sẽ xuất hiện tại đây." /></div>}
      </Card>
    </div>
  );
}

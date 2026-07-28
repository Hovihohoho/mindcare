import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { expertWorkspaceApi } from "@/features/expert-calendar";
import { Card, EmptyState, Loading } from "@/shared";

export function ExpertBookingsPage() {
  const bookings = useQuery({
    queryKey: ["expert-bookings"],
    queryFn: () => expertWorkspaceApi.bookings(),
  });

  return (
    <div className="mx-auto max-w-[1000px] space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Yêu cầu đặt lịch</h1>
        <p className="mt-2 text-slate-600">
          Danh sách lịch hẹn được lấy trực tiếp từ hệ thống.
        </p>
      </header>
      <Card className="p-6">
        {bookings.isLoading ? (
          <Loading />
        ) : bookings.isError ? (
          <p className="text-sm text-rose-600">Không thể tải danh sách lịch hẹn.</p>
        ) : bookings.data?.items.length ? (
          <div className="divide-y divide-line">
            {bookings.data.items.map((item) => (
              <article
                className="grid gap-3 py-5 md:grid-cols-[1fr_auto] md:items-center"
                key={item.id}
              >
                <div>
                  <h2 className="font-bold">Lịch hẹn #{item.id.slice(0, 8)}</h2>
                  <p className="mt-1 text-sm text-muted">Khách hàng: {item.userId}</p>
                  {item.note && <p className="mt-2 text-sm">{item.note}</p>}
                </div>
                <div className="md:text-right">
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold">
                    {item.status}
                  </span>
                  <p className="mt-2 text-sm">
                    {format(new Date(item.createdAt), "dd/MM/yyyy HH:mm")}
                  </p>
                  <p className="text-sm font-semibold">
                    {item.price.toLocaleString("vi-VN")} {item.currency}
                  </p>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Chưa có yêu cầu đặt lịch"
            description="Yêu cầu mới từ người dùng sẽ xuất hiện tại đây."
          />
        )}
      </Card>
    </div>
  );
}

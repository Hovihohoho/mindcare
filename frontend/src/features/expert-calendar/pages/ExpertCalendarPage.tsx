import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarDays, Clock3, Trash2 } from "lucide-react";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { Button, Card, EmptyState, Input, Loading } from "@/shared";
import { expertWorkspaceApi } from "../api/expert-workspace.api";

function toIso(date: string, time: string) {
  return new Date(`${date}T${time}:00`).toISOString();
}

export function ExpertCalendarPage() {
  const queryClient = useQueryClient();
  const today = format(new Date(), "yyyy-MM-dd");
  const [date, setDate] = useState(today);
  const [startTime, setStartTime] = useState("08:00");
  const [endTime, setEndTime] = useState("09:00");
  const schedules = useQuery({ queryKey: ["expert-schedules"], queryFn: expertWorkspaceApi.schedules });
  const create = useMutation({
    mutationFn: () => expertWorkspaceApi.createSchedule(toIso(date, startTime), toIso(date, endTime)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["expert-schedules"] }),
  });
  const cancel = useMutation({
    mutationFn: expertWorkspaceApi.cancelSchedule,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["expert-schedules"] }),
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    if (new Date(toIso(date, endTime)) <= new Date(toIso(date, startTime))) return;
    create.mutate();
  }

  return (
    <div className="mx-auto max-w-[1100px] space-y-7">
      <header>
        <h1 className="text-3xl font-bold">Quản lý lịch làm việc</h1>
        <p className="mt-2 text-slate-600">Tạo và theo dõi các khung giờ tư vấn trong 90 ngày tới.</p>
      </header>
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_380px]">
        <Card className="p-6">
          <h2 className="flex items-center gap-2 text-xl font-semibold"><CalendarDays className="text-brand-700" />Khung giờ của tôi</h2>
          {schedules.isLoading ? <Loading /> : schedules.isError ? (
            <p className="mt-6 text-sm text-rose-600">Không thể tải lịch làm việc. Vui lòng thử lại.</p>
          ) : schedules.data?.length ? (
            <div className="mt-5 divide-y divide-line">
              {schedules.data.map((item) => (
                <div className="flex flex-wrap items-center justify-between gap-4 py-4" key={item.id}>
                  <div>
                    <b>{format(new Date(item.startAt), "EEEE, dd/MM/yyyy", { locale: vi })}</b>
                    <p className="mt-1 text-sm text-muted">
                      {format(new Date(item.startAt), "HH:mm")} – {format(new Date(item.endAt), "HH:mm")}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold">{item.status}</span>
                    {item.status === "AVAILABLE" && (
                      <Button aria-label="Hủy khung giờ" loading={cancel.isPending} onClick={() => cancel.mutate(item.id)} size="icon" variant="ghost">
                        <Trash2 className="size-4 text-rose-600" />
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : <div className="mt-5"><EmptyState title="Chưa có khung giờ" description="Tạo khung giờ đầu tiên bằng biểu mẫu bên cạnh." /></div>}
        </Card>
        <Card className="p-6">
          <h2 className="flex items-center gap-3 text-xl font-semibold"><Clock3 className="text-brand-700" />Thêm khung giờ</h2>
          <form className="mt-6 space-y-5" onSubmit={submit}>
            <Input label="Ngày" min={today} onChange={(e) => setDate(e.target.value)} required type="date" value={date} />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Bắt đầu" onChange={(e) => setStartTime(e.target.value)} required type="time" value={startTime} />
              <Input label="Kết thúc" onChange={(e) => setEndTime(e.target.value)} required type="time" value={endTime} />
            </div>
            {endTime <= startTime && <p className="text-sm text-rose-600">Giờ kết thúc phải sau giờ bắt đầu.</p>}
            {create.isError && <p className="text-sm text-rose-600">Không thể tạo khung giờ. Thời gian có thể bị trùng hoặc không hợp lệ.</p>}
            <Button className="w-full" disabled={endTime <= startTime} loading={create.isPending} type="submit">Thêm khung giờ</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}

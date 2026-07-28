import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarDays, Clock3, Trash2 } from "lucide-react";
import { useState } from "react";
import { Button, Card, Input, Loading } from "@/shared";
import { expertScheduleApi } from "../api/expertSchedule.api";

const rangeFrom = new Date();
const rangeTo = new Date(rangeFrom);
rangeTo.setDate(rangeTo.getDate() + 30);

export function ExpertCalendarPage() {
  const queryClient = useQueryClient();
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("10:00");
  const schedules = useQuery({
    queryKey: ["expert-schedules", rangeFrom.toISOString(), rangeTo.toISOString()],
    queryFn: () => expertScheduleApi.list(rangeFrom.toISOString(), rangeTo.toISOString()),
  });
  const createSchedule = useMutation({
    mutationFn: () => expertScheduleApi.create(
      new Date(`${date}T${startTime}`).toISOString(),
      new Date(`${date}T${endTime}`).toISOString(),
    ),
    onSuccess: async () => {
      setDate("");
      await queryClient.invalidateQueries({ queryKey: ["expert-schedules"] });
    },
  });
  const removeSchedule = useMutation({
    mutationFn: expertScheduleApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["expert-schedules"] }),
  });

  return (
    <div className="mx-auto max-w-[1100px]">
      <header>
        <h1 className="text-3xl font-bold">Quản lý lịch làm việc</h1>
        <p className="mt-2 text-slate-600">Các khung giờ được lưu trực tiếp trong Booking Service.</p>
      </header>
      <div className="mt-8 grid items-start gap-6 lg:grid-cols-[1fr_380px]">
        <Card className="p-6">
          <h2 className="text-xl font-semibold">Khung giờ trong 30 ngày tới</h2>
          {schedules.isLoading ? <Loading /> : (
            <div className="mt-5 space-y-3">
              {schedules.data?.map((item) => (
                <article className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-line p-4" key={item.id}>
                  <div>
                    <p className="font-semibold">{new Date(item.startAt).toLocaleDateString("vi-VN")}</p>
                    <p className="mt-1 text-sm text-muted">
                      {new Date(item.startAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                      {" – "}
                      {new Date(item.endAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold">{item.status}</span>
                    {item.status === "AVAILABLE" && (
                      <Button size="icon" variant="ghost" aria-label="Xóa" onClick={() => removeSchedule.mutate(item.id)}>
                        <Trash2 className="size-4 text-rose-600" />
                      </Button>
                    )}
                  </div>
                </article>
              ))}
              {schedules.data?.length === 0 && <p className="py-10 text-center text-muted">Chưa có khung giờ nào.</p>}
            </div>
          )}
        </Card>
        <Card className="p-6">
          <h2 className="flex items-center gap-3 text-xl font-medium"><Clock3 className="text-blue-600" />Thêm khung giờ</h2>
          <div className="mt-6 space-y-5">
            <Input label="Chọn ngày" type="date" leading={<CalendarDays className="size-5" />} value={date} onChange={(event) => setDate(event.target.value)} />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Giờ bắt đầu" type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} />
              <Input label="Giờ kết thúc" type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} />
            </div>
          </div>
          <Button className="mt-8 w-full" size="lg" disabled={!date} loading={createSchedule.isPending} onClick={() => createSchedule.mutate()}>
            Thêm khung giờ
          </Button>
          {createSchedule.isError && <p className="mt-4 text-sm text-rose-700">Không thể tạo khung giờ. Hãy kiểm tra thời gian đã chọn.</p>}
        </Card>
      </div>
    </div>
  );
}

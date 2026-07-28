import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, ShieldCheck, Video } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button, Card, Loading, Textarea } from "@/shared";
import { bookingApi } from "../api/booking.api";
import { SchedulePicker } from "../components/SchedulePicker";

export function BookingPage() {
  const { expertId = "" } = useParams();
  const navigate = useNavigate();
  const [scheduleId, setScheduleId] = useState<string>();
  const [note, setNote] = useState("");
  const schedules = useQuery({ queryKey: ["schedules", expertId], queryFn: () => bookingApi.schedules(expertId), enabled: Boolean(expertId) });
  const selected = schedules.data?.find((item) => item.id === scheduleId);
  const create = useMutation({
    mutationFn: () => bookingApi.create(scheduleId!, note),
    onSuccess: (checkout) => navigate("/payment", { state: { checkout, selected } }),
  });

  return (
    <div>
      <Link className="flex items-center gap-2 text-sm text-slate-500" to="/experts"><ArrowLeft className="size-4" />Quay lại danh sách</Link>
      <h1 className="mt-4 text-4xl font-bold text-brand-700">Đặt lịch tư vấn</h1>
      <p className="mt-2 text-slate-500">Chọn một khung giờ khả dụng từ lịch của chuyên gia.</p>
      <div className="mt-8 grid items-start gap-6 lg:grid-cols-[1fr_370px]">
        <Card className="p-6">
          <p className="mb-5 text-sm font-semibold uppercase tracking-wide text-slate-500">Khung giờ khả dụng trong 30 ngày</p>
          {schedules.isLoading ? <Loading /> : schedules.isError ? <p className="text-rose-700">Không tải được lịch chuyên gia.</p> : <SchedulePicker schedules={schedules.data ?? []} selected={scheduleId} onSelect={setScheduleId} />}
          {!schedules.isLoading && !schedules.data?.length && <p className="text-muted">Chuyên gia chưa mở lịch.</p>}
          <div className="mt-6"><Textarea label="Lưu ý cho chuyên gia" maxLength={2000} value={note} onChange={(event) => setNote(event.target.value)} /></div>
        </Card>
        <Card className="p-6 lg:sticky lg:top-28">
          <h2 className="text-2xl font-semibold text-brand-700">Xác nhận</h2>
          <p className="mt-5 flex items-center gap-2 font-semibold"><Video className="text-brand-700" />Tư vấn trực tuyến</p>
          <dl className="mt-6 space-y-4"><div className="flex justify-between gap-4"><dt className="text-muted">Bắt đầu</dt><dd className="text-right font-semibold">{selected ? new Date(selected.startAt).toLocaleString("vi-VN") : "Chưa chọn"}</dd></div><div className="flex justify-between gap-4"><dt className="text-muted">Kết thúc</dt><dd className="text-right font-semibold">{selected ? new Date(selected.endAt).toLocaleString("vi-VN") : "Chưa chọn"}</dd></div></dl>
          {create.isError && <p className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể tạo booking. Khung giờ có thể vừa được người khác đặt.</p>}
          <Button className="mt-7 w-full" size="lg" loading={create.isPending} disabled={!scheduleId} onClick={() => create.mutate()}>Xác nhận đặt lịch</Button>
          <p className="mt-6 flex gap-2 text-xs text-muted"><ShieldCheck className="size-4 shrink-0" />Giá và trạng thái thanh toán do máy chủ xác định.</p>
        </Card>
      </div>
    </div>
  );
}

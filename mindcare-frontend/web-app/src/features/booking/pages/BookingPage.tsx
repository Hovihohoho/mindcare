import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, ShieldCheck, Star } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Avatar, Button, Card, Loading, Textarea } from "@/shared";
import { useExpert } from "@/features/expert-directory";
import { bookingApi } from "../api/booking.api";
import { SchedulePicker } from "../components/SchedulePicker";

export function BookingPage() {
  const { expertId = "" } = useParams();
  const [scheduleId, setScheduleId] = useState<string>();
  const [note, setNote] = useState("");
  const expert = useExpert(expertId);
  const schedules = useQuery({
    queryKey: ["schedules", expertId],
    queryFn: () => bookingApi.schedules(expertId),
  });
  const createBooking = useMutation({
    mutationFn: () => bookingApi.create(scheduleId!, note.trim()),
  });

  if (expert.isLoading) return <Loading />;
  if (!expert.data) return <p className="rounded-xl bg-rose-50 p-4 text-rose-700">Không tìm thấy chuyên gia.</p>;
  const selected = schedules.data?.find((item) => item.id === scheduleId);

  if (createBooking.data) {
    return (
      <Card className="mx-auto max-w-xl p-10 text-center">
        <CheckCircle2 className="mx-auto size-16 text-emerald-600" />
        <h1 className="mt-5 text-3xl font-bold">Đặt lịch thành công</h1>
        <p className="mt-3 text-muted">Mã booking: {createBooking.data.booking.id}</p>
        <p className="mt-2 font-semibold">{createBooking.data.booking.status}</p>
        <Link className="mt-7 inline-block" to="/"><Button>Về trang chủ</Button></Link>
      </Card>
    );
  }

  return (
    <div>
      <Link className="flex items-center gap-2 text-sm text-slate-500" to="/experts"><ArrowLeft className="size-4" />Quay lại danh sách</Link>
      <h1 className="mt-4 text-4xl font-bold text-brand-700">Đặt lịch tư vấn</h1>
      <p className="mt-2 text-slate-500">Khung giờ được tải trực tiếp từ Booking Service.</p>
      <div className="mt-8 grid items-start gap-6 lg:grid-cols-[1fr_370px]">
        <div className="space-y-6">
          <Card className="p-6">
            <p className="mb-5 text-sm font-semibold uppercase tracking-wide text-slate-500">Chọn khung giờ</p>
            {schedules.isLoading ? <Loading /> : <SchedulePicker schedules={schedules.data ?? []} selected={scheduleId} onSelect={setScheduleId} />}
            {!schedules.isLoading && schedules.data?.length === 0 && <p className="py-8 text-center text-muted">Chuyên gia chưa mở lịch.</p>}
          </Card>
          <Card className="p-6">
            <Textarea label="Lưu ý cho chuyên gia" value={note} onChange={(event) => setNote(event.target.value)} />
          </Card>
        </div>
        <Card className="p-6 lg:sticky lg:top-28">
          <h2 className="text-2xl font-semibold text-brand-700">Tóm tắt dịch vụ</h2>
          <div className="mt-5 flex items-center gap-3 rounded-xl border border-line p-4">
            <Avatar fallback={expert.data.displayName} />
            <div>
              <p className="font-bold">{expert.data.displayName}</p>
              <p className="text-xs text-muted">{expert.data.headline}</p>
              <p className="mt-1 text-sm"><Star className="mr-1 inline size-4 fill-amber-400 text-amber-400" /><b>{expert.data.averageRating}</b></p>
            </div>
          </div>
          <dl className="mt-6 space-y-4">
            <div className="flex justify-between"><dt className="text-muted">Thời gian:</dt><dd className="text-right font-semibold">{selected ? new Date(selected.startAt).toLocaleString("vi-VN") : "Chưa chọn"}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Phí:</dt><dd className="font-semibold">{Number(expert.data.consultationFee).toLocaleString("vi-VN")} {expert.data.currency}</dd></div>
          </dl>
          <Button className="mt-7 w-full" size="lg" disabled={!scheduleId} loading={createBooking.isPending} onClick={() => createBooking.mutate()}>
            Xác nhận đặt lịch
          </Button>
          {createBooking.isError && <p className="mt-4 text-sm text-rose-700">Không thể tạo booking. Khung giờ có thể đã được đặt.</p>}
          <p className="mt-8 flex items-center justify-center gap-2 text-xs text-muted"><ShieldCheck className="size-4" />Dữ liệu được lưu trong Booking Service</p>
        </Card>
      </div>
    </div>
  );
}

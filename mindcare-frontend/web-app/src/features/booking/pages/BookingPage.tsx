import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Check, ShieldCheck, Star, Video } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Avatar, Button, Card, Loading, MonthCalendar } from "@/shared";
import { bookingApi } from "../api/booking.api";
import { SchedulePicker } from "../components/SchedulePicker";

export function BookingPage() {
  const { expertId = "" } = useParams();
  const navigate = useNavigate();
  const [day, setDay] = useState(10);
  const [scheduleId, setScheduleId] = useState<string>();
  const schedules = useQuery({ queryKey: ["schedules", expertId], queryFn: () => bookingApi.schedules(expertId) });
  const selected = schedules.data?.find((item) => item.id === scheduleId);

  return (
    <div>
      <Link className="flex items-center gap-2 text-sm text-slate-500" to="/experts"><ArrowLeft className="size-4" />Quay lại danh sách</Link>
      <h1 className="mt-4 text-4xl font-bold text-brand-700">Đặt lịch tư vấn</h1>
      <p className="mt-2 text-slate-500">Vui lòng chọn thời gian và phương thức tư vấn phù hợp với bạn.</p>
      <div className="mt-8 grid items-start gap-6 lg:grid-cols-[1fr_370px]">
        <div className="space-y-6">
          <Card className="p-6">
            <p className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Chọn ngày</p>
            <MonthCalendar selectedDay={day} onSelectDay={setDay} />
            <div className="mt-5 flex gap-5 border-t border-line pt-4 text-xs text-muted"><span>● Ngày làm việc</span><span className="text-slate-300">● Ngày nghỉ</span></div>
          </Card>
          <Card className="p-6">
            <p className="mb-5 text-sm font-semibold uppercase tracking-wide text-slate-500">Chọn khung giờ</p>
            {schedules.isLoading ? <Loading /> : <SchedulePicker schedules={schedules.data ?? []} selected={scheduleId} onSelect={setScheduleId} />}
          </Card>
          <Card className="p-6">
            <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Hình thức tư vấn</p>
            <button className="mt-5 flex w-full items-center gap-4 rounded-xl border-2 border-brand-700 bg-sky-100 p-5 text-left"><span className="grid size-11 place-items-center rounded-full bg-sky-200 text-brand-700"><Video /></span><span className="flex-1"><b>Trực tuyến</b><small className="mt-1 block text-muted">Qua Video Call (Google Meet)</small></span><Check className="text-brand-700" /></button>
          </Card>
        </div>
        <Card className="p-6 lg:sticky lg:top-28">
          <h2 className="text-2xl font-semibold text-brand-700">Tóm tắt dịch vụ</h2>
          <div className="mt-5 flex items-center gap-3 rounded-xl border border-line p-4"><Avatar fallback="NA" /><div><p className="font-bold">TS. Nguyễn Văn A</p><p className="text-xs text-muted">Chuyên gia tâm lý lâm sàng</p><p className="mt-1 text-sm"><Star className="mr-1 inline size-4 fill-amber-400 text-amber-400" /><b>4.9</b> <span className="text-muted">(120 đánh giá)</span></p></div></div>
          <dl className="mt-6 space-y-4"><div className="flex justify-between"><dt className="text-muted">Ngày tư vấn:</dt><dd className="font-semibold">Thứ Ba, {day}/12/2024</dd></div><div className="flex justify-between"><dt className="text-muted">Giờ tư vấn:</dt><dd className="font-semibold">{selected ? new Date(selected.startAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }) : "Chưa chọn"}</dd></div><div className="flex justify-between"><dt className="text-muted">Hình thức:</dt><dd className="font-semibold">Online</dd></div><div className="border-t border-line pt-4"><div className="flex justify-between"><dt className="text-muted">Phí dịch vụ:</dt><dd>150.000đ</dd></div><div className="mt-2 flex justify-between"><dt className="text-muted">Phí nền tảng:</dt><dd>50.000đ</dd></div></div><div className="flex items-end justify-between pt-3"><dt className="text-sm text-muted">Tổng cộng</dt><dd className="text-3xl font-bold text-brand-700">200.000đ</dd></div></dl>
          <Button className="mt-7 w-full" size="lg" disabled={!scheduleId} onClick={() => navigate(`/payment?expert=${expertId}&schedule=${scheduleId}`)}>Tiếp tục thanh toán</Button>
          <button className="mt-5 w-full text-sm font-semibold text-brand-700">Hủy bỏ</button>
          <p className="mt-8 flex items-center justify-center gap-2 text-xs text-muted"><ShieldCheck className="size-4" />Thanh toán bảo mật & Mã hóa 256-bit</p>
        </Card>
      </div>
    </div>
  );
}

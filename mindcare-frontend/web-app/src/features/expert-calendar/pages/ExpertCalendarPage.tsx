import { CalendarDays, ChevronLeft, ChevronRight, Clock3 } from "lucide-react";
import { Button, Card, Input } from "@/shared";
import { WeekCalendar } from "../components/WeekCalendar";

export function ExpertCalendarPage() {
  return (
    <div className="mx-auto max-w-[1200px]">
      <header className="flex flex-wrap items-end justify-between gap-5"><div><h1 className="text-3xl font-bold">Quản lý lịch làm việc</h1><p className="mt-2 text-slate-600">Thiết lập thời gian và theo dõi các phiên tham vấn của bạn.</p></div><div className="flex items-center rounded-xl bg-blue-50 p-1"><Button size="icon" variant="ghost"><ChevronLeft /></Button><b className="rounded-lg bg-white px-5 py-3">14 Th05 - 20 Th05, 2024</b><Button size="icon" variant="ghost"><ChevronRight /></Button></div></header>
      <div className="mt-8 grid items-start gap-6 xl:grid-cols-[1fr_390px]">
        <Card className="overflow-x-auto"><WeekCalendar /></Card>
        <div className="space-y-6">
          <Card className="p-6">
            <h2 className="flex items-center gap-3 text-xl font-medium"><Clock3 className="text-blue-600" />Thiết lập khung giờ</h2>
            <div className="mt-6 space-y-5"><Input label="Chọn ngày" type="date" defaultValue="2024-05-15" leading={<CalendarDays className="size-5" />} /><div className="grid grid-cols-2 gap-4"><Input label="Giờ bắt đầu" type="time" defaultValue="08:00" /><Input label="Giờ kết thúc" type="time" defaultValue="09:00" /></div><label className="flex items-center gap-3 text-sm font-medium"><input className="size-5 rounded border-line" type="checkbox" />Lặp lại hằng tuần</label></div>
            <Button className="mt-8 w-full rounded-lg bg-blue-600 hover:bg-blue-700" size="lg">Thêm khung giờ</Button>
            <Button className="mt-2 w-full rounded-lg bg-emerald-300 text-emerald-800 hover:bg-emerald-400" size="lg">Cập nhật</Button>
            <button className="mt-5 w-full text-red-500">Xóa khung giờ</button>
          </Card>
          <div className="rounded-xl border border-blue-100 bg-blue-50 p-6"><p className="font-semibold">CHÚ THÍCH</p><p className="mt-5 flex items-center gap-3"><span className="size-4 rounded-full bg-emerald-500" />Đã mở (Available)</p><p className="mt-4 flex items-center gap-3"><span className="size-4 rounded-full bg-red-500" />Đã đặt (Booked)</p></div>
        </div>
      </div>
    </div>
  );
}

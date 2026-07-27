import { ArrowRight, ShieldCheck, Video } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card, MonthCalendar } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

const slots = ["08:00 - 09:00", "09:30 - 10:30", "11:00 - 12:00", "14:00 - 15:00", "15:30 - 16:30", "17:00 - 18:00"];

export function ExpertBookingPanel({ expert }: { expert: ExpertSummary }) {
  return (
    <aside className="space-y-5 lg:sticky lg:top-28">
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Đặt lịch tham vấn</h2>
        <p className="mt-5 text-sm font-semibold">Chọn hình thức</p>
        <button className="mt-2 flex items-center gap-2 rounded-lg border-2 border-brand-700 px-6 py-3 font-semibold text-brand-700"><Video className="size-5" />Online</button>
        <div className="mt-5"><MonthCalendar compact selectedDay={10} /></div>
        <p className="mt-5 text-sm font-semibold">Khung giờ khả dụng</p>
        <div className="mt-2 grid grid-cols-2 gap-2">{slots.map((slot, index) => <button className={index === 1 ? "rounded-lg border-2 border-brand-700 bg-sky-50 px-2 py-2 text-xs font-semibold text-brand-700" : "rounded-lg border border-line px-2 py-2 text-xs font-medium disabled:bg-slate-100 disabled:text-slate-400"} disabled={index === 5} key={slot}>{slot}</button>)}</div>
        <Link className="mt-7 block" to={`/booking/${expert.expertUserId}`}><Button className="w-full bg-blue-400 hover:bg-blue-500" size="lg">Tiếp tục thanh toán <ArrowRight className="size-5" /></Button></Link>
        <p className="mt-3 text-center text-xs text-muted">Bạn sẽ chưa bị trừ phí ở bước này.</p>
      </Card>
      <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800"><ShieldCheck className="size-5 shrink-0" />Bảo mật thông tin 100%. Mọi cuộc hội thoại đều được mã hóa đầu cuối.</div>
    </aside>
  );
}

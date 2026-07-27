import { CalendarCheck, CalendarDays, ClipboardPlus, UsersRound } from "lucide-react";
import { Card } from "@/shared";
import { AppointmentList } from "../components/AppointmentList";
import { ConsultationChart } from "../components/ConsultationChart";
import { ExpertMetricCard } from "../components/ExpertMetricCard";

const days = ["CN","T2","T3","T4","T5","T6","T7", "28","29","30","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","1"];

export function ExpertDashboardPage() {
  return (
    <div className="mx-auto max-w-[1180px] space-y-8">
      <header>
        <h1 className="text-3xl font-bold text-[#075da5]">Chào buổi sáng, Bác sĩ Nguyen Van A</h1>
        <p className="mt-2 flex flex-wrap items-center gap-2 text-sm text-slate-600">
          <CalendarDays className="size-4 text-blue-600" /> Thứ Hai, 24 Tháng 5, 2024
          <span className="text-slate-300">•</span> Hôm nay bạn có <b>8 lịch hẹn, 5 yêu cầu mới.</b>
        </p>
      </header>
      <div className="grid gap-6 md:grid-cols-3">
        <ExpertMetricCard label="Lịch hôm nay" value="08" hint="+12%" icon={<CalendarCheck />} tone="bg-blue-100 text-blue-600" />
        <ExpertMetricCard label="Yêu cầu đặt lịch mới" value="05" hint="+ 5 yêu cầu" icon={<ClipboardPlus />} tone="bg-emerald-200 text-emerald-700" />
        <ExpertMetricCard label="Tổng số ca tư vấn" value="3,560" hint="Tổng quan" icon={<UsersRound />} tone="bg-blue-100 text-blue-700" />
      </div>
      <div className="grid gap-8 xl:grid-cols-[1.45fr_1fr]">
        <Card className="p-6">
          <div className="flex justify-between"><h2 className="text-2xl font-semibold">Danh sách lịch hẹn hôm nay</h2><button className="text-sm font-semibold text-blue-600">Xem tất cả</button></div>
          <AppointmentList />
        </Card>
        <Card className="p-6">
          <h2 className="text-2xl font-semibold">Lịch tháng 05</h2>
          <div className="mt-5 grid grid-cols-7 gap-y-2 text-center text-sm">
            {days.map((value,index) => <span className={value === "10" && index > 7 ? "grid aspect-square place-items-center rounded-xl bg-[#0867ac] font-bold text-white shadow-md" : "grid aspect-square place-items-center rounded-lg text-slate-700"} key={`${value}-${index}`}>{value}</span>)}
          </div>
        </Card>
      </div>
      <Card className="p-6"><div className="flex justify-between"><div><h2 className="text-2xl font-semibold">Thống kê buổi tư vấn</h2><p className="text-sm text-slate-600">Số buổi trong 7 ngày gần nhất</p></div><span className="rounded-lg bg-[#f2f5fd] px-4 py-3 text-sm">Tuần này</span></div><ConsultationChart /></Card>
    </div>
  );
}

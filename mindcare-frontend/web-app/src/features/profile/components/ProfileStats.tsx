import { CalendarCheck, ClipboardCheck, HeartPulse } from "lucide-react";
import { Card } from "@/shared";

const stats = [
  { icon: HeartPulse, label: "Nhật ký tâm trạng", value: "", suffix: "" },
  { icon: ClipboardCheck, label: "Đánh giá hoàn thành", value: "12", suffix: "bản" },
  { icon: CalendarCheck, label: "Lịch hẹn tư vấn", value: "05", suffix: "buổi" },
];

export function ProfileStats() {
  return <div className="grid gap-4 sm:grid-cols-3">{stats.map(({ icon: Icon, ...item }, index) => <Card className="flex min-h-24 items-center gap-4 p-6" key={item.label}><span className={`grid size-12 place-items-center rounded-xl ${index === 0 ? "bg-sky-200 text-brand-700" : index === 1 ? "bg-emerald-200 text-emerald-800" : "bg-amber-200 text-amber-800"}`}><Icon /></span><div><p className="text-base text-muted">{item.label}</p>{item.value && <p className="font-semibold">{item.value} <span className="font-normal text-muted">{item.suffix}</span></p>}</div></Card>)}</div>;
}

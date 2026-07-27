import { CheckCircle2, Clock3, MoreVertical } from "lucide-react";
import { Avatar, Badge } from "@/shared";
import { todayAppointments } from "../constants/dashboard.mock";

export function AppointmentList() {
  return (
    <div className="mt-5 space-y-4">
      {todayAppointments.map((item, index) => (
        <article className={index === 0 ? "flex items-center gap-4 rounded-xl bg-[#f2f5fb] p-4" : "flex items-center gap-4 rounded-xl border border-line p-4"} key={item.id}>
          <Avatar fallback={item.name} />
          <div className="min-w-0 flex-1">
            <h3 className="truncate text-sm font-bold">{item.name}</h3>
            <p className="mt-1 flex items-center gap-1.5 text-sm text-muted"><Clock3 className="size-4" />{item.time}</p>
          </div>
          <Badge tone={item.status === "Sắp tới" ? "purple" : item.status === "Đang chờ" ? "success" : "neutral"}>{item.status}</Badge>
          {index === 0 ? <CheckCircle2 className="size-5 text-slate-600" /> : <MoreVertical className="size-5 text-slate-700" />}
        </article>
      ))}
    </div>
  );
}

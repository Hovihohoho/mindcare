import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { cn } from "@/shared";
import type { ExpertSchedule } from "../types/booking.types";

interface SchedulePickerProps {
  schedules: ExpertSchedule[];
  selected?: string;
  onSelect: (id: string) => void;
}

export function SchedulePicker({ schedules, selected, onSelect }: SchedulePickerProps) {
  const visible = schedules.slice(0, 6);
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {visible.map((slot) => (
        <button
          key={slot.id}
          disabled={slot.status !== "AVAILABLE"}
          onClick={() => onSelect(slot.id)}
          className={cn("focus-ring rounded-xl border px-4 py-4 text-sm font-semibold transition", slot.status !== "AVAILABLE" ? "cursor-not-allowed border-slate-100 bg-slate-100 text-slate-400" : selected === slot.id ? "border-2 border-brand-700 bg-sky-100 text-brand-900" : "border-line bg-white hover:border-brand-500")}
        >
          {format(new Date(slot.startAt), "HH:mm", { locale: vi })} – {format(new Date(slot.endAt), "HH:mm", { locale: vi })}
          {slot.status !== "AVAILABLE" && " (Đã đặt)"}
        </button>
      ))}
    </div>
  );
}

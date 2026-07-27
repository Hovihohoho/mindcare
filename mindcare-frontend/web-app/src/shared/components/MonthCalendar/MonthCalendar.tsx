import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/shared/lib/cn";

interface MonthCalendarProps {
  compact?: boolean;
  selectedDay?: number;
  onSelectDay?: (day: number) => void;
}

const cells = [
  25, 26, 27, 28, 29, 30, 1,
  2, 3, 4, 5, 6, 7, 8,
  9, 10, 11, 12, 13, 14, 15,
  16, 17, 18, 19, 20, 21, 22,
  23, 24, 25, 26, 27, 28, 29,
  30, 31, 1, 2, 3, 4, 5,
];

export function MonthCalendar({ compact, selectedDay = 10, onSelectDay }: MonthCalendarProps) {
  return (
    <div>
      <div className="flex items-center justify-between">
        <button className="rounded-lg p-2 hover:bg-slate-100" aria-label="Tháng trước"><ChevronLeft className="size-5" /></button>
        <b>Tháng 12, 2024</b>
        <button className="rounded-lg p-2 hover:bg-slate-100" aria-label="Tháng sau"><ChevronRight className="size-5" /></button>
      </div>
      <div className="mt-3 grid grid-cols-7 text-center text-sm font-medium text-slate-500">
        {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day) => <span className="py-2" key={day}>{day}</span>)}
      </div>
      <div className={cn("grid grid-cols-7", compact ? "gap-1" : "gap-2")}>
        {cells.map((day, index) => {
          const outside = index < 6 || index > 36;
          const weekend = index % 7 > 4;
          return (
            <button
              className={cn(
                "grid place-items-center rounded-lg text-sm transition",
                compact ? "h-9" : "h-[72px]",
                outside || weekend ? "bg-slate-50 text-slate-300" : "border border-emerald-200 bg-emerald-50 text-slate-800",
                !outside && day === selectedDay && "border-2 border-brand-700 bg-sky-200 font-bold text-brand-900",
              )}
              disabled={outside || weekend}
              key={`${day}-${index}`}
              onClick={() => onSelectDay?.(day)}
            >
              {String(day).padStart(2, "0")}
            </button>
          );
        })}
      </div>
    </div>
  );
}

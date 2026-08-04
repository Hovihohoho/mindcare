import { format, isSameDay } from "date-fns";
import { vi } from "date-fns/locale";
import { Clock3 } from "lucide-react";
import { cn } from "@/shared";
import type { ExpertSchedule } from "../api/expertSchedule.api";

const START_HOUR = 7;
const END_HOUR = 20;
const HOUR_HEIGHT = 64;
const hours = Array.from({ length: END_HOUR - START_HOUR }, (_, index) => START_HOUR + index);

interface WeeklyCalendarProps {
  days: Date[];
  schedules: ExpertSchedule[];
  selectedScheduleId?: string;
  onEmptySlotSelect: (day: Date, hour: number) => void;
  onScheduleSelect: (schedule: ExpertSchedule) => void;
}

function scheduleStyle(schedule: ExpertSchedule) {
  const start = new Date(schedule.startAt);
  const end = new Date(schedule.endAt);
  const startMinutes = start.getHours() * 60 + start.getMinutes();
  const endMinutes = end.getHours() * 60 + end.getMinutes();
  const visibleStart = START_HOUR * 60;
  const visibleEnd = END_HOUR * 60;
  const topMinutes = Math.max(startMinutes, visibleStart) - visibleStart;
  const visibleMinutes = Math.min(endMinutes, visibleEnd) - Math.max(startMinutes, visibleStart);

  return {
    top: `${(topMinutes / 60) * HOUR_HEIGHT}px`,
    height: `${Math.max((visibleMinutes / 60) * HOUR_HEIGHT, 34)}px`,
  };
}

function statusLabel(status: ExpertSchedule["status"]) {
  if (status === "AVAILABLE") return "Đã mở";
  if (status === "HELD") return "Đang giữ";
  if (status === "BOOKED") return "Đã đặt";
  return "Đã hủy";
}

export function WeeklyCalendar({
  days,
  schedules,
  selectedScheduleId,
  onEmptySlotSelect,
  onScheduleSelect,
}: WeeklyCalendarProps) {
  const today = new Date();

  return (
    <section
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
      aria-label="Lịch làm việc theo tuần"
    >
      <div className="overflow-x-auto">
        <div className="min-w-[812px]">
          <div className="grid grid-cols-[56px_repeat(7,minmax(108px,1fr))] border-b border-slate-200 bg-slate-50/70">
            <div className="flex h-16 items-center justify-center border-r border-slate-200 text-slate-500">
              <Clock3 className="size-4" aria-hidden="true" />
            </div>
            {days.map((day, index) => {
              const isToday = isSameDay(day, today);
              const isSunday = index === 6;
              return (
                <div
                  key={day.toISOString()}
                  className={cn(
                    "flex h-16 flex-col items-center justify-center border-r border-slate-200 last:border-r-0",
                    isToday && "bg-sky-50",
                  )}
                >
                  <span className={cn("text-[11px] font-semibold uppercase tracking-wide text-slate-500", isSunday && "text-rose-500")}>
                    {format(day, "EEEE", { locale: vi })}
                  </span>
                  <span
                    className={cn(
                      "mt-0.5 grid size-7 place-items-center rounded-full text-sm text-slate-800",
                      isToday && "bg-brand-700 font-semibold text-white",
                      isSunday && !isToday && "text-rose-500",
                    )}
                  >
                    {format(day, "d")}
                  </span>
                </div>
              );
            })}
          </div>

          <div className="max-h-[610px] overflow-y-auto">
            <div
              className="grid grid-cols-[56px_repeat(7,minmax(108px,1fr))]"
              style={{ height: hours.length * HOUR_HEIGHT }}
            >
              <div className="relative border-r border-slate-200" aria-hidden="true">
                {hours.map((hour) => (
                  <span
                    key={hour}
                    className="absolute right-2 -translate-y-2 text-[11px] font-medium text-slate-500"
                    style={{ top: (hour - START_HOUR) * HOUR_HEIGHT }}
                  >
                    {String(hour).padStart(2, "0")}:00
                  </span>
                ))}
              </div>

              {days.map((day) => {
                const daySchedules = schedules.filter((schedule) => (
                  schedule.status !== "CANCELLED"
                  && isSameDay(new Date(schedule.startAt), day)
                ));
                return (
                  <div
                    key={day.toISOString()}
                    className={cn(
                      "relative border-r border-slate-200 last:border-r-0",
                      isSameDay(day, today) && "bg-sky-50/55",
                    )}
                  >
                    {hours.map((hour) => (
                      <button
                        key={hour}
                        type="button"
                        className="absolute inset-x-0 border-t border-slate-200 text-left outline-none transition-colors hover:bg-sky-50 active:bg-sky-100 focus-visible:z-10 focus-visible:bg-sky-50 focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500"
                        style={{ top: (hour - START_HOUR) * HOUR_HEIGHT, height: HOUR_HEIGHT }}
                        aria-label={`Tạo khung giờ ${String(hour).padStart(2, "0")}:00, ${format(day, "dd/MM/yyyy")}`}
                        onClick={() => onEmptySlotSelect(day, hour)}
                      />
                    ))}

                    {daySchedules.map((schedule) => {
                      const isAvailable = schedule.status === "AVAILABLE";
                      const isSelected = schedule.id === selectedScheduleId;
                      const start = new Date(schedule.startAt);
                      const end = new Date(schedule.endAt);
                      return (
                        <button
                          key={schedule.id}
                          type="button"
                          className={cn(
                            "absolute inset-x-1 z-10 overflow-hidden rounded-lg px-2 py-1.5 text-left text-[11px] font-semibold leading-4 text-white shadow-sm outline-none transition-shadow hover:shadow-md active:shadow-sm focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2",
                            isAvailable ? "bg-emerald-500" : "bg-rose-500",
                            isSelected && "ring-2 ring-slate-900 ring-offset-2",
                          )}
                          style={scheduleStyle(schedule)}
                          onClick={() => onScheduleSelect(schedule)}
                          aria-label={`${statusLabel(schedule.status)}, ${format(start, "HH:mm")} đến ${format(end, "HH:mm")}`}
                        >
                          <span className="block truncate">{statusLabel(schedule.status)}</span>
                          <span className="block truncate font-normal text-white/90">
                            {format(start, "HH:mm")}–{format(end, "HH:mm")}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

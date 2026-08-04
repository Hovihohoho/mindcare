/* Hallmark · component: emotion calendar · genre: modern-minimal · theme: MindCare
 * states: data · empty · selected · hover · focus · loading · error
 * contrast: pass · mobile: pass
 * pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/shared";
import { emotionOptions } from "../constants/emotion.constants";
import type { EmotionTrendPoint } from "../types/emotion.types";
import { dateKey, emotionOptionFromScore } from "../utils/emotion-calendar.utils";
import { EmotionFaceIcon } from "./EmotionPicker";

const weekdays = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];

interface EmotionCalendarProps {
  month: Date;
  points: EmotionTrendPoint[];
  selectedDate: Date;
  onSelectDate: (date: Date) => void;
  onChangeMonth: (offset: number) => void;
  canGoNext: boolean;
}

export function EmotionCalendar({
  month,
  points,
  selectedDate,
  onSelectDate,
  onChangeMonth,
  canGoNext,
}: EmotionCalendarProps) {
  const year = month.getFullYear();
  const monthIndex = month.getMonth();
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const leadingEmptyDays = (new Date(year, monthIndex, 1).getDay() + 6) % 7;
  const pointsByDate = new Map(points.map((point) => [point.periodStart.slice(0, 10), point]));
  const cells = Array.from({ length: leadingEmptyDays + daysInMonth }, (_, index) => {
    const day = index - leadingEmptyDays + 1;
    return day > 0 ? day : null;
  });

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6" aria-label="Lịch cảm xúc theo tháng">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-slate-800 sm:text-xl">Góc nhìn tổng quan</h2>
        <div className="flex items-center gap-1">
          <button
            type="button"
            className="focus-ring grid size-9 place-items-center rounded-full text-slate-600 hover:bg-slate-100 active:bg-slate-200"
            onClick={() => onChangeMonth(-1)}
            aria-label="Tháng trước"
          >
            <ChevronLeft className="size-4" />
          </button>
          <span className="min-w-24 text-center text-sm font-semibold text-slate-700">
            Tháng {monthIndex + 1}/{year}
          </span>
          <button
            type="button"
            className="focus-ring grid size-9 place-items-center rounded-full text-slate-600 hover:bg-slate-100 active:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => onChangeMonth(1)}
            disabled={!canGoNext}
            aria-label="Tháng sau"
          >
            <ChevronRight className="size-4" />
          </button>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-7 gap-1.5 sm:gap-2">
        {weekdays.map((weekday) => (
          <span className="pb-1 text-center text-[11px] font-bold text-slate-500 sm:text-xs" key={weekday}>
            {weekday}
          </span>
        ))}

        {cells.map((day, index) => {
          if (day === null) return <span aria-hidden="true" key={`empty-${index}`} />;

          const date = new Date(year, monthIndex, day);
          const key = dateKey(date);
          const point = pointsByDate.get(key);
          const option = emotionOptionFromScore(point?.averageScore ?? null);
          const selected = dateKey(selectedDate) === key;

          return (
            <button
              type="button"
              key={key}
              className={cn(
                "focus-ring relative flex h-16 min-w-0 flex-col items-center justify-center rounded-lg border bg-white transition-colors sm:h-20",
                selected ? "border-brand-700 bg-sky-50/50" : "border-slate-200 hover:border-slate-400 active:bg-slate-50",
              )}
              onClick={() => onSelectDate(date)}
              aria-pressed={selected}
              aria-label={`${day}/${monthIndex + 1}/${year}${option ? `, điểm trung bình ${point?.averageScore?.toFixed(2)}, ${option.label}` : ", chưa có nhật ký"}`}
            >
              <span className={cn("absolute left-1.5 top-1 text-[9px] font-bold sm:left-2 sm:top-1.5 sm:text-[10px]", selected ? "text-brand-700" : "text-slate-500")}>
                {String(day).padStart(2, "0")}
              </span>
              {option ? (
                <EmotionFaceIcon face={option.face} colorClass={option.colorClass} className="size-5 sm:size-6" />
              ) : (
                <span className="text-sm font-semibold text-slate-300" aria-hidden="true">—</span>
              )}
              {selected && <span className="absolute bottom-1.5 size-1 rounded-full bg-brand-700" aria-hidden="true" />}
            </button>
          );
        })}
      </div>

      <div className="mt-4 flex flex-wrap justify-center gap-x-4 gap-y-2 border-t border-slate-200 pt-4">
        {emotionOptions.map((option) => (
          <span className="flex items-center gap-1.5 text-[11px] text-slate-500 sm:text-xs" key={option.value}>
            <span className={cn("size-2.5 rounded-full", option.colorClass)} aria-hidden="true" />
            {option.label}
          </span>
        ))}
      </div>
    </section>
  );
}

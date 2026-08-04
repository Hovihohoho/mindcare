/* Hallmark · page: emotion history · genre: modern-minimal · theme: MindCare
 * states: loading · data · empty · error · selected day
 * contrast: pass · mobile: pass
 * pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Button, Loading, PageHeader } from "@/shared";
import { emotionApi } from "../api/emotion.api";
import { EmotionCalendar } from "../components/EmotionCalendar";
import { EmotionFaceIcon } from "../components/EmotionPicker";
import { emotionOptions } from "../constants/emotion.constants";
import { dateKey, emotionOptionFromScore } from "../utils/emotion-calendar.utils";

function startOfLocalDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + amount);
}

function periodLabel(date: Date) {
  const hour = date.getHours();
  if (hour < 11) return "Sáng";
  if (hour < 14) return "Trưa";
  if (hour < 18) return "Chiều";
  return "Tối";
}

function monthInputValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

export function EmotionHistoryPage() {
  const today = useMemo(() => startOfLocalDay(new Date()), []);
  const [month, setMonth] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1));
  const [selectedDate, setSelectedDate] = useState(today);

  const monthStart = useMemo(
    () => new Date(month.getFullYear(), month.getMonth(), 1),
    [month],
  );
  const monthEnd = useMemo(
    () => new Date(month.getFullYear(), month.getMonth() + 1, 1),
    [month],
  );
  const selectedStart = useMemo(() => startOfLocalDay(selectedDate), [selectedDate]);
  const selectedEnd = useMemo(() => addDays(selectedDate, 1), [selectedDate]);

  const trends = useQuery({
    queryKey: ["emotion-trends", monthStart.toISOString(), monthEnd.toISOString()],
    queryFn: () => emotionApi.trends(monthStart.toISOString(), monthEnd.toISOString()),
  });
  const dayHistory = useQuery({
    queryKey: ["emotion-history-day", dateKey(selectedDate)],
    queryFn: () => emotionApi.history(selectedStart.toISOString(), selectedEnd.toISOString(), 100),
  });

  const monthlyAverage = useMemo(() => {
    const available = (trends.data ?? []).filter(
      (point) => point.averageScore !== null && point.count > 0,
    );
    const totalEntries = available.reduce((sum, point) => sum + point.count, 0);
    if (totalEntries === 0) return null;
    return available.reduce(
      (sum, point) => sum + (point.averageScore ?? 0) * point.count,
      0,
    ) / totalEntries;
  }, [trends.data]);
  const monthlyEmotion = emotionOptionFromScore(monthlyAverage);

  const changeMonth = (nextMonth: Date) => {
    const normalized = new Date(nextMonth.getFullYear(), nextMonth.getMonth(), 1);
    const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    if (normalized > currentMonth) return;
    setMonth(normalized);
    const isCurrentMonth = normalized.getFullYear() === today.getFullYear()
      && normalized.getMonth() === today.getMonth();
    setSelectedDate(isCurrentMonth ? today : normalized);
  };

  const selectedDateLabel = selectedDate.toLocaleDateString("vi-VN");

  return (
    <div className="min-w-0 space-y-6">
      <nav className="text-xs font-medium text-slate-500" aria-label="Điều hướng">
        <Link className="hover:text-brand-700" to="/emotion">Nhật ký cảm xúc</Link>
        <span className="mx-2" aria-hidden="true">/</span>
        <span className="font-semibold text-brand-700">Lịch sử cảm xúc</span>
      </nav>

      <PageHeader
        title="Lịch sử cảm xúc"
        description="Theo dõi hành trình tâm trí của bạn qua từng ngày."
        actions={(
          <>
            <label className="sr-only" htmlFor="emotion-history-month">Chọn tháng</label>
            <input
              id="emotion-history-month"
              type="month"
              value={monthInputValue(month)}
              max={monthInputValue(today)}
              onChange={(event) => {
                const [year, monthNumber] = event.target.value.split("-").map(Number);
                if (year && monthNumber) changeMonth(new Date(year, monthNumber - 1, 1));
              }}
              className="focus-ring h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700"
            />
            <Link to="/emotion"><Button>Ghi nhật ký</Button></Link>
          </>
        )}
      />

      <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(19rem,1fr)]">
        <div className="min-w-0 space-y-4">
          {trends.isLoading ? (
            <div className="grid min-h-[32rem] place-items-center rounded-xl border border-slate-200 bg-white"><Loading /></div>
          ) : trends.isError ? (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700">
              Không thể tải dữ liệu cảm xúc của tháng này.
            </div>
          ) : (
            <EmotionCalendar
              month={month}
              points={trends.data ?? []}
              selectedDate={selectedDate}
              onSelectDate={setSelectedDate}
              onChangeMonth={(offset) => changeMonth(new Date(month.getFullYear(), month.getMonth() + offset, 1))}
              canGoNext={month < new Date(today.getFullYear(), today.getMonth(), 1)}
            />
          )}

          <section className="flex min-h-20 items-center gap-3 rounded-xl border border-sky-300 bg-sky-50 p-4" aria-label="Điểm cảm xúc trung bình tháng">
            {monthlyEmotion ? (
              <>
                <EmotionFaceIcon face={monthlyEmotion.face} colorClass={monthlyEmotion.colorClass} className="size-9" />
                <div className="min-w-0">
                  <h2 className="text-sm font-bold text-sky-900">Điểm trung bình tháng</h2>
                  <p className="mt-0.5 text-sm text-sky-900">
                    <span className="font-bold">{monthlyAverage?.toFixed(2)}</span>
                    {" · "}
                    {monthlyEmotion.label}
                  </p>
                </div>
              </>
            ) : (
              <p className="text-sm text-sky-900">Tháng này chưa có dữ liệu để tính điểm trung bình.</p>
            )}
          </section>
        </div>

        <aside className="min-h-[28rem] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm xl:min-h-[43rem]" aria-labelledby="day-detail-title">
          <div className="border-b border-slate-200 px-4 py-4">
            <h2 id="day-detail-title" className="text-lg font-bold text-slate-800 sm:text-xl">
              Chi tiết ngày {selectedDateLabel}
            </h2>
          </div>

          <div className="max-h-[38rem] space-y-3 overflow-y-auto p-4">
            {dayHistory.isLoading && <Loading />}
            {dayHistory.isError && (
              <p className="rounded-lg bg-rose-50 p-4 text-sm text-rose-700">Không thể tải nhật ký của ngày đã chọn.</p>
            )}
            {dayHistory.data?.items.map((entry) => {
              const option = emotionOptions.find((item) => item.value === entry.emotionType);
              const createdAt = new Date(entry.createdAt);
              return (
                <article
                  className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm"
                  key={entry.id}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-sm font-bold text-slate-800">
                        {periodLabel(createdAt)}, {createdAt.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                      </h3>
                      <p className="mt-1 text-xs text-slate-500">{option?.label ?? entry.emotionType}</p>
                    </div>
                    {option && <EmotionFaceIcon face={option.face} colorClass={option.colorClass} className="size-5" />}
                  </div>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">
                    {entry.content || "Không có ghi chú."}
                  </p>
                </article>
              );
            })}
            {!dayHistory.isLoading && !dayHistory.isError && dayHistory.data?.items.length === 0 && (
              <div className="grid min-h-64 place-items-center px-4 text-center">
                <div>
                  <p className="font-semibold text-slate-700">Chưa có nhật ký trong ngày này</p>
                  <p className="mt-2 text-sm leading-6 text-slate-500">Chọn một ngày khác hoặc ghi lại cảm xúc mới.</p>
                </div>
              </div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

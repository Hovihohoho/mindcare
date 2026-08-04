import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addDays,
  addWeeks,
  endOfWeek,
  format,
  isBefore,
  isSameWeek,
  startOfDay,
  startOfWeek,
} from "date-fns";
import { CalendarDays, ChevronLeft, ChevronRight, Clock3, Plus, RefreshCw, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Button, Card, Input, Loading, cn } from "@/shared";
import { Avatar } from "@/shared";
import { clientRecordApi } from "@/features/client-record";
import { expertScheduleApi, type ExpertSchedule } from "../api/expertSchedule.api";
import { WeeklyCalendar } from "../components/WeeklyCalendar";

const defaultStart = "09:00";
const defaultEnd = "10:00";

function inputDate(date: Date) {
  return format(date, "yyyy-MM-dd");
}

function inputTime(date: Date) {
  return format(date, "HH:mm");
}

function toIso(date: string, time: string) {
  return new Date(`${date}T${time}:00`).toISOString();
}

function getErrorMessage(error: unknown) {
  const apiError = error as {
    response?: { status?: number; data?: { code?: string; detail?: string } };
  };
  const status = apiError.response?.status;
  const code = apiError.response?.data?.code;

  if (status === 409 || code === "SCHEDULE_OVERLAP") {
    return "Khung giờ này bị trùng với một lịch đang có.";
  }
  if (status === 403) {
    return "Tài khoản chuyên gia chưa đủ điều kiện để mở lịch.";
  }
  return apiError.response?.data?.detail ?? "Không thể lưu khung giờ. Vui lòng kiểm tra lại thời gian.";
}

export function ExpertCalendarPage() {
  const queryClient = useQueryClient();
  const today = useMemo(() => new Date(), []);
  const initialDate = useMemo(() => addDays(today, 1), [today]);
  const [weekStart, setWeekStart] = useState(() => startOfWeek(today, { weekStartsOn: 1 }));
  const [date, setDate] = useState(inputDate(initialDate));
  const [startTime, setStartTime] = useState(defaultStart);
  const [endTime, setEndTime] = useState(defaultEnd);
  const [repeatWeekly, setRepeatWeekly] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState<ExpertSchedule | null>(null);
  const [actionMessage, setActionMessage] = useState("");

  const weekEnd = endOfWeek(weekStart, { weekStartsOn: 1 });
  const days = useMemo(
    () => Array.from({ length: 7 }, (_, index) => addDays(weekStart, index)),
    [weekStart],
  );
  const rangeFrom = startOfDay(weekStart).toISOString();
  const rangeTo = startOfDay(addDays(weekEnd, 1)).toISOString();

  const schedules = useQuery({
    queryKey: ["expert-schedules", rangeFrom, rangeTo],
    queryFn: () => expertScheduleApi.list(rangeFrom, rangeTo),
  });
  const bookedClient = useQuery({
    queryKey: ["expert-client-by-schedule", selectedSchedule?.id],
    queryFn: () => clientRecordApi.bySchedule(selectedSchedule!.id),
    enabled: selectedSchedule?.status === "BOOKED",
  });

  const resetEditor = () => {
    setSelectedSchedule(null);
    setRepeatWeekly(false);
    setActionMessage("");
  };

  const invalidateSchedules = () => queryClient.invalidateQueries({ queryKey: ["expert-schedules"] });

  const createSchedule = useMutation({
    mutationFn: async () => {
      const occurrences = repeatWeekly ? 4 : 1;
      return Promise.all(
        Array.from({ length: occurrences }, (_, index) => {
          const occurrenceDate = addWeeks(new Date(`${date}T00:00:00`), index);
          return expertScheduleApi.create(
            toIso(inputDate(occurrenceDate), startTime),
            toIso(inputDate(occurrenceDate), endTime),
          );
        }),
      );
    },
    onSuccess: async () => {
      setActionMessage(repeatWeekly ? "Đã tạo lịch lặp lại trong 4 tuần." : "Đã thêm khung giờ.");
      setRepeatWeekly(false);
      await invalidateSchedules();
    },
  });

  const updateSchedule = useMutation({
    mutationFn: () => expertScheduleApi.update(
      selectedSchedule!.id,
      toIso(date, startTime),
      toIso(date, endTime),
    ),
    onSuccess: async () => {
      setSelectedSchedule(null);
      setActionMessage("Đã cập nhật khung giờ.");
      await invalidateSchedules();
    },
  });

  const removeSchedule = useMutation({
    mutationFn: (scheduleId: string) => expertScheduleApi.remove(scheduleId),
    onSuccess: async () => {
      setSelectedSchedule(null);
      setActionMessage("Đã xóa khung giờ.");
      await invalidateSchedules();
    },
  });

  const isEditable = !selectedSchedule || selectedSchedule.status === "AVAILABLE";
  const selectedStart = new Date(`${date}T${startTime}:00`);
  const selectedEnd = new Date(`${date}T${endTime}:00`);
  const isPast = !isBefore(new Date(), selectedStart);
  const isInvalidRange = !isBefore(selectedStart, selectedEnd);
  const validationMessage = !isEditable
    ? ""
    : isPast
      ? "Thời gian bắt đầu phải ở tương lai."
      : isInvalidRange
        ? "Giờ kết thúc phải sau giờ bắt đầu."
        : "";
  const mutationError = selectedSchedule
    ? updateSchedule.error ?? removeSchedule.error
    : createSchedule.error;

  const selectEmptySlot = (day: Date, hour: number) => {
    const slotStart = new Date(day);
    slotStart.setHours(hour, 0, 0, 0);
    const slotEnd = new Date(slotStart);
    slotEnd.setHours(hour + 1);
    setSelectedSchedule(null);
    setDate(inputDate(day));
    setStartTime(inputTime(slotStart));
    setEndTime(inputTime(slotEnd));
    setActionMessage("");
    createSchedule.reset();
    updateSchedule.reset();
    removeSchedule.reset();
  };

  const selectSchedule = (schedule: ExpertSchedule) => {
    const start = new Date(schedule.startAt);
    const end = new Date(schedule.endAt);
    setSelectedSchedule(schedule);
    setDate(inputDate(start));
    setStartTime(inputTime(start));
    setEndTime(inputTime(end));
    setRepeatWeekly(false);
    setActionMessage("");
    createSchedule.reset();
    updateSchedule.reset();
    removeSchedule.reset();
  };

  const goToDateWeek = (nextDate: string) => {
    setDate(nextDate);
    const parsed = new Date(`${nextDate}T00:00:00`);
    if (!isSameWeek(parsed, weekStart, { weekStartsOn: 1 })) {
      setWeekStart(startOfWeek(parsed, { weekStartsOn: 1 }));
    }
  };

  return (
    <div className="mx-auto max-w-[1440px]">
      <header className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 md:text-3xl">Quản lý lịch làm việc</h1>
          <p className="mt-1 text-sm text-slate-600 md:text-base">
            Thiết lập thời gian và theo dõi các phiên tham vấn của bạn.
          </p>
        </div>

        <div className="flex w-fit items-center gap-1 rounded-xl bg-sky-100/80 p-1 shadow-sm" aria-label="Điều hướng tuần">
          <button
            type="button"
            className="focus-ring grid size-9 place-items-center rounded-lg text-slate-700 transition-colors hover:bg-white/70 active:bg-white"
            onClick={() => setWeekStart((current) => addWeeks(current, -1))}
            aria-label="Tuần trước"
          >
            <ChevronLeft className="size-4" />
          </button>
          <button
            type="button"
            className="focus-ring h-9 whitespace-nowrap rounded-lg bg-white px-3 text-xs font-medium text-slate-800 shadow-sm transition-colors hover:bg-slate-50 active:bg-slate-100 sm:px-4 sm:text-sm"
            onClick={() => setWeekStart(startOfWeek(today, { weekStartsOn: 1 }))}
          >
            {format(weekStart, "dd/MM")} – {format(weekEnd, "dd/MM/yyyy")}
          </button>
          <button
            type="button"
            className="focus-ring grid size-9 place-items-center rounded-lg text-slate-700 transition-colors hover:bg-white/70 active:bg-white"
            onClick={() => setWeekStart((current) => addWeeks(current, 1))}
            aria-label="Tuần sau"
          >
            <ChevronRight className="size-4" />
          </button>
        </div>
      </header>

      <div className="mt-6 grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="min-w-0">
          {schedules.isLoading ? (
            <Card className="grid min-h-[520px] place-items-center">
              <Loading />
            </Card>
          ) : schedules.isError ? (
            <Card className="grid min-h-[420px] place-items-center p-8 text-center">
              <div>
                <p className="font-semibold text-slate-900">Không thể tải lịch làm việc</p>
                <p className="mt-2 text-sm text-slate-600">Hãy kiểm tra kết nối tới Booking Service rồi thử lại.</p>
                <Button className="mt-5 rounded-xl" variant="outline" leftIcon={<RefreshCw className="size-4" />} onClick={() => schedules.refetch()}>
                  Thử lại
                </Button>
              </div>
            </Card>
          ) : (
            <WeeklyCalendar
              days={days}
              schedules={schedules.data ?? []}
              selectedScheduleId={selectedSchedule?.id}
              onEmptySlotSelect={selectEmptySlot}
              onScheduleSelect={selectSchedule}
            />
          )}
          <p className="mt-3 text-xs text-slate-500 xl:hidden">Vuốt ngang trong lịch để xem đủ 7 ngày.</p>
        </div>

        <aside className="space-y-4 xl:sticky xl:top-24">
          <Card className="rounded-2xl p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="flex items-center gap-2 text-base font-semibold text-slate-900">
                  <Clock3 className="size-5 text-brand-600" />
                  {selectedSchedule ? "Chi tiết khung giờ" : "Thiết lập khung giờ"}
                </h2>
                <p className="mt-1 text-xs text-slate-500">
                  {selectedSchedule
                    ? selectedSchedule.status === "AVAILABLE"
                      ? "Bạn có thể cập nhật hoặc xóa lịch đang mở."
                      : "Lịch đã đặt chỉ có thể xem."
                    : "Chọn ô trên lịch hoặc nhập thời gian."}
                </p>
              </div>
              {selectedSchedule && (
                <button type="button" className="focus-ring rounded-lg px-2 py-1 text-xs font-medium text-brand-700 hover:bg-brand-50" onClick={resetEditor}>
                  Tạo mới
                </button>
              )}
            </div>

            <div className="mt-5 space-y-4">
              {selectedSchedule?.status === "BOOKED" && (
                <div className="rounded-xl border border-sky-200 bg-sky-50 p-3">
                  {bookedClient.isLoading ? (
                    <div className="flex items-center gap-3 py-1 text-sm text-slate-600">
                      <span className="size-5 animate-spin rounded-full border-2 border-sky-200 border-t-brand-700" />
                      Đang tải người đặt lịch...
                    </div>
                  ) : bookedClient.data ? (
                    <Link
                      className="focus-ring flex items-center gap-3 rounded-lg p-1 transition-colors hover:bg-white active:bg-sky-100"
                      to={`/expert/clients/${bookedClient.data.userId}`}
                    >
                      <Avatar className="size-10 bg-white text-sm" fallback={bookedClient.data.fullName} />
                      <span className="min-w-0 flex-1">
                        <span className="block text-xs text-slate-500">Người đặt lịch</span>
                        <span className="block truncate text-sm font-semibold text-slate-900">
                          {bookedClient.data.fullName}
                        </span>
                      </span>
                      <span className="whitespace-nowrap text-xs font-semibold text-brand-700">Xem hồ sơ</span>
                    </Link>
                  ) : (
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs leading-5 text-rose-700">
                        Không thể tải thông tin người đặt lịch.
                      </p>
                      <button
                        type="button"
                        className="focus-ring whitespace-nowrap rounded-lg px-2 py-1 text-xs font-semibold text-brand-700 hover:bg-white active:bg-sky-100"
                        onClick={() => bookedClient.refetch()}
                      >
                        Thử lại
                      </button>
                    </div>
                  )}
                </div>
              )}

              <Input
                label="Chọn ngày"
                type="date"
                min={inputDate(today)}
                leading={<CalendarDays className="size-4" />}
                className="disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
                value={date}
                disabled={!isEditable}
                onChange={(event) => goToDateWeek(event.target.value)}
              />
              <div className="grid grid-cols-2 gap-3">
                <Input className="disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400" label="Giờ bắt đầu" type="time" value={startTime} disabled={!isEditable} onChange={(event) => setStartTime(event.target.value)} />
                <Input className="disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400" label="Giờ kết thúc" type="time" value={endTime} disabled={!isEditable} onChange={(event) => setEndTime(event.target.value)} />
              </div>

              {!selectedSchedule && (
                <label className="flex cursor-pointer items-start gap-3 py-1 text-sm font-medium text-slate-700">
                  <input
                    type="checkbox"
                    className="mt-0.5 size-4 rounded border-slate-300 accent-brand-700"
                    checked={repeatWeekly}
                    onChange={(event) => setRepeatWeekly(event.target.checked)}
                  />
                  <span>
                    Lặp lại hàng tuần
                    <span className="mt-0.5 block text-xs font-normal text-slate-500">Tạo cùng khung giờ trong 4 tuần.</span>
                  </span>
                </label>
              )}
            </div>

            {(validationMessage || mutationError || actionMessage) && (
              <p
                className={cn(
                  "mt-4 rounded-lg px-3 py-2 text-xs leading-5",
                  validationMessage || mutationError
                    ? "bg-rose-50 text-rose-700"
                    : "bg-emerald-50 text-emerald-700",
                )}
                role="status"
              >
                {validationMessage || (mutationError ? getErrorMessage(mutationError) : actionMessage)}
              </p>
            )}

            <div className="mt-5 space-y-2">
              {!selectedSchedule && (
                <Button
                  className="w-full rounded-xl"
                  leftIcon={<Plus className="size-4" />}
                  disabled={Boolean(validationMessage)}
                  loading={createSchedule.isPending}
                  onClick={() => createSchedule.mutate()}
                >
                  Thêm khung giờ
                </Button>
              )}
              {selectedSchedule?.status === "AVAILABLE" && (
                <>
                  <Button
                    className="w-full rounded-xl"
                    variant="secondary"
                    disabled={Boolean(validationMessage)}
                    loading={updateSchedule.isPending}
                    onClick={() => updateSchedule.mutate()}
                  >
                    Cập nhật
                  </Button>
                  <Button
                    className="w-full rounded-xl text-rose-600 hover:bg-rose-50"
                    variant="ghost"
                    leftIcon={<Trash2 className="size-4" />}
                    loading={removeSchedule.isPending}
                    onClick={() => removeSchedule.mutate(selectedSchedule.id)}
                  >
                    Xóa khung giờ
                  </Button>
                </>
              )}
            </div>
          </Card>

          <section className="rounded-2xl border border-slate-200 bg-sky-50/70 p-5" aria-labelledby="calendar-legend">
            <h2 id="calendar-legend" className="text-xs font-semibold uppercase tracking-wider text-slate-600">Chú thích</h2>
            <div className="mt-4 space-y-3 text-sm text-slate-700">
              <p className="flex items-center gap-3"><span className="size-3 rounded-full bg-emerald-500" />Đã mở (Available)</p>
              <p className="flex items-center gap-3"><span className="size-3 rounded-full bg-rose-500" />Đã đặt / đang giữ</p>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

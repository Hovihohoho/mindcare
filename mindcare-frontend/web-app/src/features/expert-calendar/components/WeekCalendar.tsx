import { calendarDays, calendarEvents, calendarHours } from "../constants/calendar.mock";

export function WeekCalendar() {
  return (
    <div className="min-w-[820px]">
      <div className="grid grid-cols-[60px_repeat(7,1fr)] border-b border-line">
        <span />
        {calendarDays.map((day, index) => {
          const [label, date] = day.split("|");
          return <div className={`border-l border-line p-3 text-center ${index === 1 ? "bg-blue-50" : ""}`} key={day}><p className={index === 6 ? "text-xs font-medium text-red-500" : "text-xs font-medium text-slate-600"}>{label}</p><p className={index === 1 ? "mt-1 text-lg font-semibold text-blue-600" : index === 6 ? "mt-1 text-lg text-red-500" : "mt-1 text-lg"}>{date}</p></div>;
        })}
      </div>
      <div className="relative grid grid-cols-[60px_repeat(7,1fr)]">
        <div>{calendarHours.map((hour) => <div className="h-16 border-b border-line pr-3 pt-2 text-right text-xs text-muted" key={hour}>{hour}</div>)}</div>
        {calendarDays.map((day, index) => <div className={index === 1 ? "relative border-l border-line bg-blue-50/60" : "relative border-l border-line"} key={day}>{calendarHours.map((hour) => <div className="h-16 border-b border-line" key={hour} />)}</div>)}
        {calendarEvents.map((event) => <div className={`absolute rounded-lg p-2 text-xs font-semibold shadow-sm ${event.tone}`} key={event.id} style={{ left: `calc(60px + (100% - 60px) * ${event.day} / 7 + 4px)`, top: `${event.start * 64 + 4}px`, width: "calc((100% - 60px) / 7 - 8px)", height: `${event.span * 64 - 8}px` }}>{event.title}</div>)}
      </div>
    </div>
  );
}

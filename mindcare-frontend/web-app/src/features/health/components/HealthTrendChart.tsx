import { format } from "date-fns";
import { vi } from "date-fns/locale";
import type { HealthTrendPoint } from "../types/health.types";

interface HealthTrendChartProps {
  points: HealthTrendPoint[];
  valueFormatter(value: number): string;
}

export function HealthTrendChart({ points, valueFormatter }: HealthTrendChartProps) {
  const maximum = Math.max(...points.map((point) => point.value), 1);

  return (
    <div className="overflow-x-auto pb-1">
      <div className="flex h-64 min-w-[38rem] items-end gap-2" role="img" aria-label="Biểu đồ chỉ số sức khỏe theo ngày">
        {points.map((point) => {
          const height = Math.max(4, (point.value / maximum) * 176);
          const label = format(new Date(point.periodStart), "dd/MM", { locale: vi });
          return (
            <div className="group flex min-w-10 flex-1 flex-col items-center justify-end" key={point.periodStart}>
              <div className="mb-2 min-h-5 text-center text-[11px] font-semibold text-slate-600 opacity-0 transition group-hover:opacity-100 group-focus-within:opacity-100">
                {valueFormatter(point.value)}
              </div>
              <button
                className="focus-ring w-full max-w-12 rounded-t-lg bg-brand-500/80 transition hover:bg-brand-600"
                style={{ height }}
                title={`${label}: ${valueFormatter(point.value)}`}
                type="button"
                aria-label={`${label}: ${valueFormatter(point.value)}`}
              />
              <span className="mt-2 text-[11px] text-slate-500">{label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

import type { EmotionTrendPoint } from "../types/emotion.types";

export function EmotionTrendChart({ points }: { points: EmotionTrendPoint[] }) {
  if (points.length === 0) {
    return <p className="mt-6 rounded-xl bg-slate-50 p-8 text-center text-sm text-muted">Chưa có dữ liệu xu hướng.</p>;
  }

  return (
    <div className="mt-6 flex h-52 items-end gap-3 md:gap-6">
      {points.map((point) => (
        <div className="flex h-full flex-1 flex-col justify-end text-center" key={point.periodStart}>
          <div
            className="mx-auto w-full max-w-16 rounded-t-xl bg-brand-500"
            style={{ height: `${Math.max(4, Number(point.averageScore) * 18)}%` }}
            title={`${point.averageScore}/5 (${point.count} bản ghi)`}
          />
          <span className="mt-3 text-xs font-semibold text-muted">
            {new Date(point.periodStart).toLocaleDateString("vi-VN", { weekday: "short" })}
          </span>
        </div>
      ))}
    </div>
  );
}

/* Hallmark · component: emotion trend chart · genre: modern-minimal · theme: MindCare
 * states: data · empty bucket · hover · focus
 * contrast: pass · mobile: pass
 * pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { cn } from "@/shared";
import type { EmotionTrendPoint } from "../types/emotion.types";

const SCORE_MIN = -2;
const SCORE_MAX = 2;
const SCORE_RANGE = SCORE_MAX - SCORE_MIN;

const axisLevels = [
  { score: 2, label: "Rất vui" },
  { score: 1, label: "Vui" },
  { score: 0, label: "Bình thường" },
  { score: -1, label: "Buồn" },
  { score: -2, label: "Căng thẳng" },
] as const;

function clampScore(value: number | null) {
  if (value === null || !Number.isFinite(value)) return SCORE_MIN;
  return Math.max(SCORE_MIN, Math.min(SCORE_MAX, value));
}

function scoreColor(score: number) {
  if (score >= 2) return "bg-green-500";
  if (score >= 1) return "bg-lime-500";
  if (score >= 0) return "bg-blue-500";
  if (score >= -1) return "bg-yellow-400";
  return "bg-rose-500";
}

function scoreHeight(score: number) {
  return Math.max(3, ((score - SCORE_MIN) / SCORE_RANGE) * 100);
}

function weekdayLabel(value: string) {
  const day = new Date(value).getDay();
  return day === 0 ? "CN" : `T${day + 1}`;
}

export function EmotionTrendChart({ points }: { points: EmotionTrendPoint[] }) {
  if (points.length === 0) {
    return <p className="mt-6 rounded-xl bg-slate-50 p-8 text-center text-sm text-muted">Chưa có dữ liệu xu hướng.</p>;
  }

  return (
    <figure className="mt-7 min-w-0" aria-label="Biểu đồ xu hướng cảm xúc từ âm 2 đến 2">
      <div className="grid min-w-0 grid-cols-[2.5rem_minmax(0,1fr)] gap-x-2 sm:grid-cols-[6.5rem_minmax(0,1fr)] sm:gap-x-4">
        <div className="relative h-64" aria-hidden="true">
          {axisLevels.map((level, index) => (
            <div
              key={level.score}
              className={cn(
                "absolute right-0 flex items-center gap-1.5 text-[11px] font-medium text-slate-500 sm:text-xs",
                index > 0 && index < axisLevels.length - 1 && "-translate-y-1/2",
                index === axisLevels.length - 1 && "-translate-y-full",
              )}
              style={{ top: `${index * 25}%` }}
            >
              <span className="hidden sm:inline">{level.label}</span>
              <span className="w-5 text-right tabular-nums text-slate-400">{level.score}</span>
            </div>
          ))}
        </div>

        <div className="relative h-64 min-w-0 border-l border-slate-200">
          {axisLevels.map((level, index) => (
            <span
              key={level.score}
              className="pointer-events-none absolute inset-x-0 border-t border-slate-200/80"
              style={{ top: `${index * 25}%` }}
              aria-hidden="true"
            />
          ))}

          <div className="absolute inset-0 flex min-w-0 items-end gap-2 px-2 sm:gap-4 sm:px-4 md:gap-6">
            {points.map((point) => {
              const score = clampScore(point.averageScore);
              const empty = point.averageScore === null || point.count === 0;
              return (
                <div className="flex h-full min-w-0 flex-1 items-end justify-center" key={point.periodStart}>
                  <div
                    className={cn(
                      "w-full max-w-16 rounded-t-xl transition-[filter] duration-150 hover:brightness-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-700",
                      scoreColor(score),
                    )}
                    style={{ height: `${scoreHeight(score)}%` }}
                    title={empty ? "Chưa ghi nhận cảm xúc · mặc định -2" : `${score.toFixed(2)} (${point.count} bản ghi)`}
                    role="img"
                    tabIndex={0}
                    aria-label={empty
                      ? `${weekdayLabel(point.periodStart)}: chưa ghi nhận cảm xúc, hiển thị mặc định âm 2`
                      : `${weekdayLabel(point.periodStart)}: điểm cảm xúc ${score.toFixed(2)}, ${point.count} bản ghi`}
                  />
                </div>
              );
            })}
          </div>
        </div>

        <span aria-hidden="true" />
        <div className="mt-3 flex min-w-0 gap-2 px-2 sm:gap-4 sm:px-4 md:gap-6">
          {points.map((point) => (
            <span className="min-w-0 flex-1 text-center text-xs font-semibold text-slate-500" key={point.periodStart}>
              {weekdayLabel(point.periodStart)}
            </span>
          ))}
        </div>
      </div>

      <figcaption className="sr-only">
        Điểm cảm xúc từ -2 đến 2. Bucket chưa có dữ liệu được hiển thị ở mức -2.
      </figcaption>
    </figure>
  );
}

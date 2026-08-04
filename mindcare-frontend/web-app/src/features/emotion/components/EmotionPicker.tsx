/* Hallmark · component: emotion picker · genre: modern-minimal · theme: MindCare
 * states: default · hover · focus · active · disabled · loading · error · success
 * contrast: pass
 * pre-emit critique: P5 H4 E5 S5 R5 V4
 */
import { cn } from "@/shared";
import { emotionOptions, type EmotionFace, type EmotionOption } from "../constants/emotion.constants";
import type { EmotionLevel } from "../types/emotion.types";

export function EmotionFaceIcon({
  face,
  colorClass,
  className,
}: {
  face: EmotionFace;
  colorClass: string;
  className?: string;
}) {
  return (
    <span className={cn("grid size-7 shrink-0 place-items-center rounded-full", colorClass, className)} aria-hidden="true">
      <svg className="size-full" viewBox="0 0 28 28" fill="none">
        {face === "laugh" && (
          <>
            <path d="m8.1 10.5 2-2 2 2" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            <path d="m15.9 10.5 2-2 2 2" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            <path d="M7.7 15.2h12.6c-.8 4-3 5.8-6.3 5.8s-5.5-1.8-6.3-5.8Z" fill="white" />
            <path d="M10.1 17.4h7.8" stroke="currentColor" strokeOpacity=".16" strokeWidth="1" />
          </>
        )}
        {face === "smile" && (
          <>
            <circle cx="9.5" cy="10.5" r="1.6" fill="white" />
            <circle cx="18.5" cy="10.5" r="1.6" fill="white" />
            <path d="M8.6 15.6c1.3 2.3 3.1 3.4 5.4 3.4s4.1-1.1 5.4-3.4" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </>
        )}
        {face === "neutral" && (
          <>
            <circle cx="9.5" cy="10.5" r="1.6" fill="white" />
            <circle cx="18.5" cy="10.5" r="1.6" fill="white" />
            <path d="M10 17h8" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </>
        )}
        {face === "sad" && (
          <>
            <circle cx="9.5" cy="10.5" r="1.6" fill="white" />
            <circle cx="18.5" cy="10.5" r="1.6" fill="white" />
            <path d="M9 19c1.3-2.1 3-3.1 5-3.1s3.7 1 5 3.1" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </>
        )}
        {face === "stressed" && (
          <>
            <path d="m7.8 8.7 3.5 3.5m0-3.5-3.5 3.5M16.7 8.7l3.5 3.5m0-3.5-3.5 3.5" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
            <path d="M8.8 19c1.3-2.2 3-3.3 5.2-3.3s3.9 1.1 5.2 3.3" stroke="white" strokeWidth="2" strokeLinecap="round" />
          </>
        )}
      </svg>
    </span>
  );
}

export function EmotionHistoryIcon({ option }: { option?: EmotionOption }) {
  if (!option) return null;
  return (
    <span className={cn("grid size-12 shrink-0 place-items-center rounded-full", option.surfaceClass)}>
      <EmotionFaceIcon face={option.face} colorClass={option.colorClass} className="size-8" />
    </span>
  );
}

export function EmotionPicker({ value, onChange }: { value?: EmotionLevel; onChange: (value: EmotionLevel) => void }) {
  return (
    <div className="flex flex-wrap justify-center gap-3 sm:gap-4 md:justify-start md:gap-7" role="radiogroup" aria-label="Chọn tâm trạng hôm nay">
      {emotionOptions.map((option) => {
        const selected = value === option.value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            aria-label={option.label}
            className={cn(
              "focus-ring flex h-[120px] w-[105px] shrink-0 flex-col items-center rounded-2xl border border-slate-200 bg-white px-3 pt-4 text-slate-500 transition-colors duration-150",
              "hover:border-slate-400 active:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-55",
              selected && cn("border-transparent shadow-sm ring-2 ring-offset-2", option.ringClass),
            )}
            onClick={() => onChange(option.value)}
          >
            <EmotionFaceIcon face={option.face} colorClass={option.colorClass} />
            <span className="mt-2 text-center text-sm font-medium leading-5 tracking-tight">
              {option.shortLines.map((line) => <span className="block" key={line}>{line}</span>)}
            </span>
          </button>
        );
      })}
    </div>
  );
}

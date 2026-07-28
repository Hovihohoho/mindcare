import { cn } from "@/shared";
import { emotionOptions } from "../constants/emotion.constants";
import type { EmotionLevel } from "../types/emotion.types";

export function EmotionPicker({ value, onChange }: { value?: EmotionLevel; onChange: (value: EmotionLevel) => void }) {
  return (
    <div className="grid grid-cols-5 gap-2 md:gap-4">
      {emotionOptions.map((option) => (
        <button
          key={option.value}
          className={cn("focus-ring flex flex-col items-center rounded-2xl border px-2 py-4 transition", value === option.value ? "border-brand-600 bg-brand-50 shadow-sm" : "border-line bg-white hover:-translate-y-0.5")}
          onClick={() => onChange(option.value)}
        >
          <span className="text-3xl md:text-4xl">{option.emoji}</span>
          <span className="mt-2 text-[11px] font-bold text-slate-700 md:text-sm">{option.label}</span>
        </button>
      ))}
    </div>
  );
}

import { cn } from "@/shared";
import type { AssessmentQuestion } from "../types/assessment.types";

interface QuestionPanelProps {
  question: AssessmentQuestion;
  value?: string;
  onChange: (value: string) => void;
}

export function QuestionPanel({ question, value, onChange }: QuestionPanelProps) {
  return (
    <div>
      <h2 className="text-balance text-3xl font-bold leading-[1.35] text-slate-900 md:text-4xl">{question.questionText}</h2>
      <div className="mt-8 space-y-3">
        {question.answerOptions.map((option) => (
          <button
            key={option.id}
            className={cn("focus-ring flex min-h-16 w-full items-center gap-4 rounded-xl border px-5 py-4 text-left text-base font-medium transition", value === option.id ? "border-brand-700 bg-sky-50 text-brand-900" : "border-line bg-white text-slate-700 hover:border-brand-500")}
            onClick={() => onChange(option.id)}
          >
            <span className={cn("size-5 rounded-full border-[5px]", value === option.id ? "border-brand-600 bg-white" : "border-slate-300")} />
            {option.optionText}
          </button>
        ))}
      </div>
    </div>
  );
}

import { forwardRef, type TextareaHTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, id, ...props }, ref) => (
    <label className="block space-y-2" htmlFor={id ?? props.name}>
      {label && <span className="text-sm font-semibold text-slate-700">{label}</span>}
      <textarea
        ref={ref}
        id={id ?? props.name}
        className={cn(
          "focus-ring min-h-28 w-full resize-y rounded-xl border border-line bg-white p-3 text-sm placeholder:text-slate-400 focus:border-brand-500",
          error && "border-rose-400",
          className,
        )}
        {...props}
      />
      {error && <span className="block text-xs text-rose-600">{error}</span>}
    </label>
  ),
);

Textarea.displayName = "Textarea";

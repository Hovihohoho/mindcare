import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leading?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, leading, id, ...props }, ref) => {
    const inputId = id ?? props.name;
    return (
      <label className="block space-y-2" htmlFor={inputId}>
        {label && <span className="text-sm font-semibold text-slate-700">{label}</span>}
        <span className="relative block">
          {leading && <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">{leading}</span>}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              "focus-ring h-12 w-full rounded-xl border border-line bg-slate-50 px-4 text-sm text-slate-800 placeholder:text-slate-400",
              "focus:border-brand-500",
              leading && "pl-10",
              error && "border-rose-400",
              className,
            )}
            {...props}
          />
        </span>
        {(error || hint) && <span className={cn("block text-xs", error ? "text-rose-600" : "text-muted")}>{error ?? hint}</span>}
      </label>
    );
  },
);

Input.displayName = "Input";

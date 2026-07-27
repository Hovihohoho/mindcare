import { Link } from "react-router-dom";
import { cn } from "@/shared/lib/cn";

interface LogoProps {
  compact?: boolean;
  className?: string;
  to?: string;
}

export function Logo({ compact, className, to = "/" }: LogoProps) {
  return (
    <Link className={cn("focus-ring inline-flex shrink-0 items-center rounded-lg", className)} to={to} aria-label="MindCare">
      {compact ? (
        <span className="text-xl font-bold text-brand-700">MC</span>
      ) : (
        <img className="h-10 w-[195px] object-contain object-left" src="/assets/mindcare-logo.png" alt="MindCare" />
      )}
    </Link>
  );
}

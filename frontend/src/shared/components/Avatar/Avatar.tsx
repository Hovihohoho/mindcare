import { UserRound } from "lucide-react";
import { cn } from "@/shared/lib/cn";

interface AvatarProps {
  src?: string;
  alt?: string;
  fallback?: string;
  className?: string;
}

export function Avatar({ src, alt = "", fallback, className }: AvatarProps) {
  return (
    <span className={cn("inline-flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand-100 font-bold text-brand-700", className)}>
      {src ? <img className="size-full object-cover" src={src} alt={alt} /> : fallback ? fallback.slice(0, 2).toUpperCase() : <UserRound className="size-1/2" />}
    </span>
  );
}

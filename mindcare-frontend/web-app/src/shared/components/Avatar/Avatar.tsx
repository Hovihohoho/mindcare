import { UserRound } from "lucide-react";
import { cn } from "@/shared/lib/cn";

interface AvatarProps {
  src?: string;
  alt?: string;
  fallback?: string;
  className?: string;
}

export function Avatar({ src, alt = "", fallback, className }: AvatarProps) {
  const resolvedSrc = src?.startsWith("/")
    ? `${import.meta.env.VITE_API_URL ?? "http://localhost:8079"}${src}`
    : src;

  return (
    <span className={cn("inline-flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand-100 font-bold text-brand-700", className)}>
      {resolvedSrc ? <img className="size-full object-cover" src={resolvedSrc} alt={alt} /> : fallback ? fallback.slice(0, 2).toUpperCase() : <UserRound className="size-1/2" />}
    </span>
  );
}

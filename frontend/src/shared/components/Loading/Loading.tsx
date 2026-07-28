import { LoaderCircle } from "lucide-react";

export function Loading({ label = "Đang tải..." }: { label?: string }) {
  return (
    <div className="flex min-h-52 items-center justify-center gap-3 text-sm text-muted" role="status">
      <LoaderCircle className="size-5 animate-spin text-brand-600" />
      {label}
    </div>
  );
}

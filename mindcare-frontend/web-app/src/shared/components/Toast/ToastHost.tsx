import { useEffect, useState } from "react";
import { CheckCircle2, Info, X, XCircle } from "lucide-react";
import { cn } from "@/shared/lib/cn";
import type { ToastDetail } from "@/shared/lib/feedback";

interface Toast extends ToastDetail { id: number }

export function ToastHost() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    const receive = (event: Event) => {
      const detail = (event as CustomEvent<ToastDetail>).detail;
      const id = Date.now() + Math.random();
      setToasts((items) => [...items.slice(-3), { id, ...detail }]);
      window.setTimeout(() => setToasts((items) => items.filter((item) => item.id !== id)), 5000);
    };
    window.addEventListener("mindcare:toast", receive);
    return () => window.removeEventListener("mindcare:toast", receive);
  }, []);

  return (
    <div className="fixed right-4 top-4 z-[100] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">
      {toasts.map((toast) => {
        const Icon = toast.kind === "success" ? CheckCircle2 : toast.kind === "error" ? XCircle : Info;
        return (
          <div className={cn("flex items-start gap-3 rounded-xl border bg-white p-4 shadow-xl", toast.kind === "error" && "border-rose-200", toast.kind === "success" && "border-emerald-200")} key={toast.id}>
            <Icon className={cn("mt-0.5 size-5 shrink-0", toast.kind === "error" ? "text-rose-600" : toast.kind === "success" ? "text-emerald-600" : "text-brand-600")} />
            <p className="flex-1 text-sm font-medium text-slate-700">{toast.message}</p>
            <button aria-label="Đóng thông báo" onClick={() => setToasts((items) => items.filter((item) => item.id !== toast.id))}><X className="size-4 text-muted" /></button>
          </div>
        );
      })}
    </div>
  );
}

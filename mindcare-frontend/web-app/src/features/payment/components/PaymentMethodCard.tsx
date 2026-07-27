import type { ReactNode } from "react";
import { cn } from "@/shared";

interface PaymentMethodCardProps {
  active: boolean;
  title: string;
  description: string;
  icon: ReactNode;
  onClick: () => void;
}

export function PaymentMethodCard({ active, title, description, icon, onClick }: PaymentMethodCardProps) {
  return (
    <button className={cn("focus-ring flex w-full items-center gap-4 rounded-xl border p-4 text-left transition", active ? "border-brand-600 bg-brand-50" : "border-line bg-white")} onClick={onClick}>
      <span className="grid size-11 place-items-center rounded-xl bg-white text-brand-600 shadow-sm">{icon}</span>
      <span className="flex-1"><b className="block">{title}</b><small className="mt-1 block text-muted">{description}</small></span>
      <span className={cn("size-5 rounded-full border-[5px]", active ? "border-brand-600" : "border-slate-300")} />
    </button>
  );
}

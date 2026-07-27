import type { ReactNode } from "react";
import { TrendingUp } from "lucide-react";
import { Card } from "@/shared";

interface ExpertMetricCardProps {
  label: string;
  value: string;
  hint: string;
  icon: ReactNode;
  tone: string;
}

export function ExpertMetricCard({ label, value, hint, icon, tone }: ExpertMetricCardProps) {
  return (
    <Card className="min-h-[190px] p-6">
      <div className="flex items-start justify-between">
        <span className={`grid size-12 place-items-center rounded-xl ${tone}`}>{icon}</span>
        <span className="flex items-center gap-1 text-xs font-semibold text-emerald-700">{hint}<TrendingUp className="size-3" /></span>
      </div>
      <p className="mt-4 text-sm font-medium uppercase text-slate-600">{label}</p>
      <p className="mt-1 text-4xl font-bold text-slate-900">{value}</p>
    </Card>
  );
}

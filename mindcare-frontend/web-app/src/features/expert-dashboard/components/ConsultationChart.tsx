import { weeklyConsultations } from "../constants/dashboard.mock";

export function ConsultationChart() {
  const labels = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];
  return <div className="mt-5 flex h-52 items-end gap-4">{weeklyConsultations.map((value, index) => <div className="flex h-full flex-1 flex-col justify-end text-center" key={labels[index]}><div className="mx-auto w-full max-w-10 rounded-t-lg bg-brand-100 hover:bg-brand-600" style={{ height: `${value * 10}%` }} /><span className="mt-2 text-xs text-muted">{labels[index]}</span></div>)}</div>;
}

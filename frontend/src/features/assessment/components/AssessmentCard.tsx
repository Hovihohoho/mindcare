import { BrainCircuit, ListChecks } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card } from "@/shared";
import type { Assessment } from "../types/assessment.types";

export function AssessmentCard({ assessment }: { assessment: Assessment }) {
  return (
    <Card className="group flex min-h-[350px] flex-col p-6">
      <div className="flex items-start justify-between">
        <span className="grid size-12 place-items-center rounded-xl bg-sky-100 text-brand-700"><BrainCircuit className="size-5" /></span>
        <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-bold text-brand-700">v{assessment.assessmentVersion}</span>
      </div>
      <h3 className="mt-5 text-lg font-semibold text-slate-800">{assessment.title}</h3>
      <p className="mt-3 flex-1 leading-6 text-muted">{assessment.description}</p>
      <div className="mt-5 flex gap-5 text-sm text-muted">
        <span className="flex items-center gap-1.5"><ListChecks className="size-4" />{assessment.code}</span>
      </div>
      <Link className="mt-7" to={`/assessments/${assessment.code}`}><Button className="w-full rounded-lg bg-emerald-700 hover:bg-emerald-800">Bắt đầu làm bài</Button></Link>
    </Card>
  );
}

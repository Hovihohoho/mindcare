import { BadgeCheck, BrainCircuit, ExternalLink, ListChecks } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card } from "@/shared";
import { BookmarkButton } from "@/features/bookmark";
import type { Assessment } from "../types/assessment.types";

export function AssessmentCard({ assessment }: { assessment: Assessment }) {
  return (
    <Card className="group flex min-h-[350px] flex-col p-6">
      <div className="flex items-start justify-between">
        <span className="grid size-12 place-items-center rounded-xl bg-sky-100 text-brand-700"><BrainCircuit className="size-5" /></span>
        <BookmarkButton type="ASSESSMENT" targetId={assessment.code} />
      </div>
      <h3 className="mt-5 text-lg font-semibold text-slate-800">{assessment.title}</h3>
      <p className="mt-3 flex-1 leading-6 text-muted">{assessment.description}</p>
      <div className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">
        <p className="flex items-center gap-2 font-semibold"><BadgeCheck className="size-4" />Nguồn đã được công khai</p>
        <p className="mt-1 text-xs leading-5">{assessment.evidence.publisher} · {assessment.evidence.publicationYear}</p>
        <a className="mt-2 inline-flex items-center gap-1 font-semibold text-emerald-800 underline" href={assessment.evidence.sourceUrl} target="_blank" rel="noreferrer">
          Xem tài liệu gốc <ExternalLink className="size-3.5" />
        </a>
      </div>
      <div className="mt-5 flex gap-5 text-sm text-muted">
        <span className="flex items-center gap-1.5"><ListChecks className="size-4" />{assessment.code}</span>
        <span>Phiên bản {assessment.assessmentVersion}</span>
      </div>
      <Link className="mt-7" to={`/assessments/${assessment.code}`}><Button className="w-full rounded-lg bg-emerald-700 hover:bg-emerald-800">Bắt đầu làm bài</Button></Link>
    </Card>
  );
}

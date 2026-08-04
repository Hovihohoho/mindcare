import { BadgeCheck, BriefcaseBusiness, MapPin, Star } from "lucide-react";
import { Avatar, Badge, Card } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

export function ExpertProfileHeader({ expert }: { expert: ExpertSummary }) {
  return (
    <Card className="p-7">
      <div className="flex flex-col gap-6 md:flex-row md:items-center">
        <div className="relative">
          <Avatar className="size-36 border-4 border-white text-3xl shadow-md" fallback={expert.displayName} src={expert.avatarUrl} />
          <span className="absolute bottom-1 right-1 grid size-7 place-items-center rounded-full bg-emerald-500 text-white ring-4 ring-white"><BadgeCheck className="size-4" /></span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div><h1 className="text-3xl font-bold text-slate-900">{expert.displayName}</h1><p className="mt-2 text-lg font-semibold text-brand-700">{expert.headline}</p></div>
            <span className="flex items-center gap-2 rounded-full bg-slate-100 px-4 py-2 font-semibold"><Star className="size-5 fill-amber-400 text-amber-400" />{expert.averageRating} <small className="font-normal text-muted">({expert.reviewCount} đánh giá)</small></span>
          </div>
          <div className="mt-4 flex flex-wrap gap-5 text-sm text-slate-500">
            <span className="flex items-center gap-2"><BriefcaseBusiness className="size-5 text-brand-700" />{expert.yearsOfExperience} năm kinh nghiệm · {expert.consultationCount} buổi tư vấn</span>
            <span className="flex items-center gap-2"><MapPin className="size-5 text-brand-700" />{expert.location}</span>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">{expert.specialties.map((value) => <Badge key={value}>{value}</Badge>)}</div>
        </div>
      </div>
    </Card>
  );
}

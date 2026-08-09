import type { MouseEvent } from "react";
import { Clock3, MessageSquareText, Star, UsersRound } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Card } from "@/shared";
import { BookmarkButton } from "@/features/bookmark";
import type { ExpertSummary } from "../types/expert.types";

export function ExpertCard({ expert }: { expert: ExpertSummary }) {
  const navigate = useNavigate();
  const profilePath = `/experts/${expert.expertUserId}`;

  const openExpertProfile = (event: MouseEvent<HTMLDivElement>) => {
    if ((event.target as HTMLElement).closest("a, button")) return;
    navigate(profilePath);
  };

  return (
    <Card
      className="group flex h-[510px] min-h-[510px] cursor-pointer flex-col overflow-hidden rounded-xl"
      onClick={openExpertProfile}
    >
      <div className="relative h-[232px] shrink-0 overflow-hidden bg-sky-50">
        <img className="size-full object-cover object-[35%_45%] transition duration-500 group-hover:scale-105" src={expert.avatarUrl || "/assets/mindcare-wellness-illustration.png"} alt="" />
        <BookmarkButton className="absolute right-4 top-4" type="EXPERT" targetId={expert.expertUserId} />
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <div className="flex h-8 min-w-0 items-center justify-between gap-3">
          <h3 className="min-w-0 flex-1 truncate text-xl font-semibold" title={expert.displayName}>{expert.displayName}</h3>
          <span className="flex shrink-0 items-center gap-1 text-sm font-semibold text-amber-500"><Star className="size-4 fill-current" />{expert.averageRating}</span>
        </div>
        <p className="mt-1 h-5 truncate text-sm font-semibold text-emerald-700" title={expert.headline}>{expert.headline}</p>
        <div className="mt-3 flex h-[76px] shrink-0 flex-wrap content-start gap-2 overflow-hidden text-xs text-slate-500 [mask-image:linear-gradient(to_bottom,#000_72%,transparent_100%)] [-webkit-mask-image:linear-gradient(to_bottom,#000_72%,transparent_100%)]">
          <span className="flex h-9 min-w-0 max-w-full items-center gap-1.5 rounded-full bg-slate-100 px-3">
            <Clock3 className="size-4 shrink-0" /><span className="truncate">{expert.yearsOfExperience} năm kinh nghiệm</span>
          </span>
          <span className="flex h-9 min-w-0 max-w-full items-center gap-1.5 rounded-full bg-slate-100 px-3">
            <UsersRound className="size-4 shrink-0" /><span className="truncate">{expert.specialties.at(-1)}</span>
          </span>
        </div>
        <div className="mt-auto flex shrink-0 gap-3 pt-4">
          <Link className="flex-1" to={profilePath}><Button className="w-full">Xem hồ sơ</Button></Link>
          <button
            type="button"
            className="focus-ring grid size-11 shrink-0 place-items-center rounded-full border border-brand-700 text-brand-700 hover:bg-brand-50 active:bg-brand-100"
            aria-label={`Chat với ${expert.displayName}`}
            onClick={(event) => {
              event.stopPropagation();
              navigate(`/chat?expertId=${expert.expertUserId}`);
            }}
          >
            <MessageSquareText className="size-5" />
          </button>
        </div>
      </div>
    </Card>
  );
}

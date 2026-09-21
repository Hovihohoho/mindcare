import { ArrowRight, BookOpen } from "lucide-react";
import { Link } from "react-router-dom";
import { BookmarkButton } from "@/features/bookmark/components/BookmarkButton";
import { Card } from "@/shared";
import type { SelfCareContent } from "../api/selfCare.api";

export function SelfCareCard({ item }: { item: SelfCareContent }) {
  const excerpt = item.content.replace(/\s+/g, " ").trim().slice(0, 170);
  return <Card className="relative flex h-full flex-col p-6">
    <BookmarkButton className="absolute right-4 top-4" type="SELF_CARE_CONTENT" targetId={item.id} />
    <BookOpen className="size-8 text-brand-600" />
    <h2 className="mt-5 pr-10 text-xl font-bold text-slate-800">{item.title}</h2>
    <p className="mt-3 line-clamp-3 flex-1 text-sm leading-6 text-muted">{excerpt}{item.content.length > excerpt.length ? "…" : ""}</p>
    {item.publisher && <p className="mt-4 text-xs font-semibold uppercase tracking-wide text-slate-500">Nguồn: {item.publisher}</p>}
    <Link className="mt-5 inline-flex items-center gap-2 font-semibold text-brand-700" to={`/self-care/${item.id}`}>Đọc nội dung <ArrowRight className="size-4" /></Link>
  </Card>;
}

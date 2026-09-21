import { useQuery } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { useParams } from "react-router-dom";
import { BookmarkButton } from "@/features/bookmark/components/BookmarkButton";
import { Card, Loading, PageHeader } from "@/shared";
import { selfCareApi } from "../api/selfCare.api";

export function SelfCareDetailPage() {
  const { id = "" } = useParams();
  const content = useQuery({ queryKey: ["self-care-content", id], queryFn: () => selfCareApi.find(id), enabled: Boolean(id) });
  if (content.isLoading) return <Loading />;
  if (!content.data) return <Card className="p-10 text-center text-rose-700">Không tìm thấy nội dung hoặc nội dung chưa được xuất bản.</Card>;
  const item = content.data;
  return <div className="mx-auto max-w-4xl space-y-6">
    <div className="flex items-start justify-between gap-4"><PageHeader title={item.title} description={item.publisher ? `Nguồn kiểm duyệt: ${item.publisher}` : "Nội dung tự chăm sóc"} /><BookmarkButton className="shrink-0" type="SELF_CARE_CONTENT" targetId={item.id} /></div>
    <Card className="p-7 md:p-10">
      <div className="whitespace-pre-wrap text-[16px] leading-8 text-slate-700">{item.content}</div>
      {item.limitation && <p className="mt-8 rounded-xl bg-amber-50 p-4 text-sm leading-6 text-amber-900">{item.limitation}</p>}
      <a className="mt-6 inline-flex items-center gap-2 font-semibold text-brand-700" href={item.sourceUrl} rel="noreferrer" target="_blank">Xem nguồn gốc <ExternalLink className="size-4" /></a>
    </Card>
  </div>;
}

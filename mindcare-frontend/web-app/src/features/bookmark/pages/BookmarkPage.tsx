import { useQuery } from "@tanstack/react-query";
import { Bookmark } from "lucide-react";
import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { AssessmentCard } from "@/features/assessment/components/AssessmentCard";
import { Card, Loading, PageHeader } from "@/shared";
import { bookmarkApi } from "../api/bookmark.api";

export function BookmarkPage() {
  const bookmarks = useQuery({ queryKey: ["bookmarks"], queryFn: bookmarkApi.list });
  const assessments = useQuery({ queryKey: ["bookmarked-assessments-source"], queryFn: assessmentApi.list });
  if (bookmarks.isLoading || assessments.isLoading) return <Loading />;
  const saved = (assessments.data ?? []).filter((item) =>
    bookmarks.data?.some((bookmark) => bookmark.targetType === "ASSESSMENT" && bookmark.targetId === item.code));
  return <div className="space-y-10">
    <PageHeader title="Nội dung đã lưu" description="Các bài đánh giá bạn muốn xem lại." />
    {saved.length === 0 && <Card className="p-10 text-center"><Bookmark className="mx-auto size-10 text-muted" /><p className="mt-4 text-muted">Bạn chưa lưu nội dung nào.</p></Card>}
    {saved.length > 0 && <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{saved.map((item) => <AssessmentCard key={item.code} assessment={item} />)}</div>}
  </div>;
}

import { useQuery } from "@tanstack/react-query";
import { Bookmark } from "lucide-react";
import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { AssessmentCard } from "@/features/assessment/components/AssessmentCard";
import { selfCareApi } from "@/features/self-care/api/selfCare.api";
import { SelfCareCard } from "@/features/self-care/components/SelfCareCard";
import { Card, Loading, PageHeader } from "@/shared";
import { bookmarkApi } from "../api/bookmark.api";

export function BookmarkPage() {
  const bookmarks = useQuery({ queryKey: ["bookmarks"], queryFn: bookmarkApi.list });
  const assessments = useQuery({ queryKey: ["bookmarked-assessments-source"], queryFn: assessmentApi.list });
  const selfCare = useQuery({ queryKey: ["self-care-content"], queryFn: selfCareApi.list });
  if (bookmarks.isLoading || assessments.isLoading || selfCare.isLoading) return <Loading />;

  const savedAssessments = (assessments.data ?? []).filter((item) => bookmarks.data?.some(
    (bookmark) => bookmark.targetType === "ASSESSMENT" && bookmark.targetId === item.code));
  const savedSelfCare = (selfCare.data ?? []).filter((item) => bookmarks.data?.some(
    (bookmark) => bookmark.targetType === "SELF_CARE_CONTENT" && bookmark.targetId === item.id));

  return <div className="space-y-10">
    <PageHeader title="Nội dung đã lưu" description="Các bài đánh giá và nội dung tự chăm sóc bạn muốn xem lại." />
    {savedAssessments.length === 0 && savedSelfCare.length === 0 && <Card className="p-10 text-center">
      <Bookmark className="mx-auto size-10 text-muted" /><p className="mt-4 text-muted">Bạn chưa lưu nội dung nào.</p>
    </Card>}
    {savedAssessments.length > 0 && <div><h2 className="mb-5 text-xl font-bold">Bài đánh giá</h2><div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{savedAssessments.map((item) => <AssessmentCard key={item.code} assessment={item} />)}</div></div>}
    {savedSelfCare.length > 0 && <div><h2 className="mb-5 text-xl font-bold">Góc tự chăm sóc</h2><div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{savedSelfCare.map((item) => <SelfCareCard key={item.id} item={item} />)}</div></div>}
  </div>;
}

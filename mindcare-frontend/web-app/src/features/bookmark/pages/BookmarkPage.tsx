import { useQuery } from "@tanstack/react-query";
import { Bookmark } from "lucide-react";
import { Link } from "react-router-dom";
import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { AssessmentCard } from "@/features/assessment/components/AssessmentCard";
import { expertApi } from "@/features/expert-directory/api/expert.api";
import { ExpertCard } from "@/features/expert-directory/components/ExpertCard";
import { Button, Card, Loading, PageHeader } from "@/shared";
import { bookmarkApi } from "../api/bookmark.api";

export function BookmarkPage() {
  const authenticated = Boolean(localStorage.getItem("mindcare.accessToken"));
  const bookmarks = useQuery({ queryKey: ["bookmarks"], queryFn: bookmarkApi.list, enabled: authenticated });
  const experts = useQuery({ queryKey: ["bookmarked-experts-source"], queryFn: () => expertApi.list(), enabled: authenticated });
  const assessments = useQuery({ queryKey: ["bookmarked-assessments-source"], queryFn: assessmentApi.list, enabled: authenticated });

  if (!authenticated) {
    return <Card className="mx-auto max-w-lg p-10 text-center"><Bookmark className="mx-auto size-10 text-muted" /><p className="mt-4 text-muted">Đăng nhập để đồng bộ nội dung đã lưu trên các thiết bị.</p><Link className="mt-5 inline-block" to="/login"><Button>Đăng nhập</Button></Link></Card>;
  }
  if (bookmarks.isLoading || experts.isLoading || assessments.isLoading) return <Loading />;

  const savedExperts = (experts.data?.items ?? []).filter((item) => bookmarks.data?.some((bookmark) => bookmark.targetType === "EXPERT" && bookmark.targetId === item.expertUserId));
  const savedAssessments = (assessments.data ?? []).filter((item) => bookmarks.data?.some((bookmark) => bookmark.targetType === "ASSESSMENT" && bookmark.targetId === item.code));
  const empty = savedExperts.length === 0 && savedAssessments.length === 0;

  return (
    <div className="space-y-10">
      <PageHeader title="Nội dung đã lưu" description="Chuyên gia và bài đánh giá bạn muốn xem lại." />
      {empty && <Card className="p-10 text-center"><Bookmark className="mx-auto size-10 text-muted" /><p className="mt-4 text-muted">Bạn chưa lưu nội dung nào.</p></Card>}
      {savedExperts.length > 0 && <section><h2 className="mb-5 text-xl font-bold text-slate-800">Chuyên gia đã lưu</h2><div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{savedExperts.map((item) => <ExpertCard key={item.expertUserId} expert={item} />)}</div></section>}
      {savedAssessments.length > 0 && <section><h2 className="mb-5 text-xl font-bold text-slate-800">Bài đánh giá đã lưu</h2><div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{savedAssessments.map((item) => <AssessmentCard key={item.code} assessment={item} />)}</div></section>}
    </div>
  );
}

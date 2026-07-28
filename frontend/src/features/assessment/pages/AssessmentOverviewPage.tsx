import { useQuery } from "@tanstack/react-query";
import { ArrowRight, BadgeCheck, LockKeyhole } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Card, Loading } from "@/shared";
import { assessmentApi } from "../api/assessment.api";
import { AssessmentCard } from "../components/AssessmentCard";
import { useAssessments } from "../hooks/useAssessments";

export function AssessmentOverviewPage() {
  const assessments = useAssessments();
  const history = useQuery({
    queryKey: ["assessment-results", "recent"],
    queryFn: () => assessmentApi.history(
      new Date(Date.now() - 365 * 86_400_000).toISOString(),
      new Date(Date.now() + 86_400_000).toISOString(),
    ),
  });

  return (
    <div className="space-y-16">
      <section className="grid items-center gap-10 lg:grid-cols-[1fr_1.05fr]">
        <div>
          <h1 className="text-4xl font-bold leading-tight text-brand-700 md:text-5xl">Đánh giá tâm lý</h1>
          <p className="mt-4 max-w-[630px] text-lg leading-7 text-slate-500">Các bài sàng lọc được quản lý theo phiên bản. Kết quả chỉ mang tính tham khảo và không thay thế chẩn đoán chuyên môn.</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <span className="inline-flex items-center gap-2 rounded-full bg-emerald-200 px-4 py-2 text-emerald-800"><BadgeCheck className="size-5" />Chấm điểm phía máy chủ</span>
            <span className="inline-flex items-center gap-2 rounded-full bg-sky-200 px-4 py-2 text-brand-700"><LockKeyhole className="size-5" />Dữ liệu thuộc tài khoản của bạn</span>
          </div>
        </div>
        <div className="h-[285px] overflow-hidden rounded-3xl border border-line bg-white shadow-xl"><img className="size-full object-cover opacity-40" src="/assets/mindcare-wellness-illustration.png" alt="" /></div>
      </section>
      <section>
        <div className="mb-6 flex items-center justify-between"><h2 className="text-lg font-medium">Bài đánh giá đã công bố</h2><Link className="flex items-center gap-2 text-brand-700" to="/assessments/library">Xem tất cả <ArrowRight className="size-5" /></Link></div>
        {assessments.isLoading ? <Loading /> : assessments.isError ? <p className="rounded-xl bg-rose-50 p-4 text-rose-700">Không tải được danh sách bài đánh giá.</p> : <div className="grid gap-6 lg:grid-cols-3">{assessments.data?.slice(0, 3).map((item) => <AssessmentCard key={item.id} assessment={item} />)}</div>}
      </section>
      <section>
        <h2 className="border-b border-line pb-4 text-lg font-semibold text-brand-700">Lịch sử làm bài</h2>
        <Card className="mt-6 overflow-hidden">
          {history.isLoading ? <div className="p-8"><Loading /></div> : history.data?.items.length ? history.data.items.map((item) => (
            <div className="grid gap-3 border-t border-line px-8 py-5 first:border-t-0 md:grid-cols-[1.25fr_1fr_1fr] md:items-center" key={item.resultId}>
              <b>{item.assessmentCode} · v{item.assessmentVersion}</b>
              <span className="text-slate-500">{new Date(item.createdAt).toLocaleString("vi-VN")}</span>
              <Badge className="w-fit" tone={item.riskLevel === "NORMAL" ? "success" : "warning"}>{item.riskLevel} · {item.totalScore} điểm</Badge>
            </div>
          )) : <p className="p-8 text-center text-muted">Chưa có kết quả đánh giá.</p>}
        </Card>
      </section>
    </div>
  );
}

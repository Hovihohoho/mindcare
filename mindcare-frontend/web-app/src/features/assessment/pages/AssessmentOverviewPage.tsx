import { useQuery } from "@tanstack/react-query";
import { ArrowRight, BadgeCheck, LockKeyhole } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Card, Loading } from "@/shared";
import { assessmentApi } from "../api/assessment.api";
import { AssessmentCard } from "../components/AssessmentCard";
import { useAssessments } from "../hooks/useAssessments";

const historyTo = new Date();
const historyFrom = new Date(historyTo);
historyFrom.setFullYear(historyFrom.getFullYear() - 1);

export function AssessmentOverviewPage() {
  const assessments = useAssessments();
  const history = useQuery({
    queryKey: ["assessment-history", historyFrom.toISOString(), historyTo.toISOString()],
    queryFn: () => assessmentApi.history(historyFrom.toISOString(), historyTo.toISOString()),
  });

  return (
    <div className="space-y-16">
      <section className="grid items-center gap-10 lg:grid-cols-[1fr_1.05fr]">
        <div>
          <h1 className="text-4xl font-bold leading-tight text-brand-700 md:text-5xl">Đánh giá tâm lý</h1>
          <p className="mt-4 max-w-[630px] text-lg leading-7 text-slate-500">
            Các bài sàng lọc đang được xuất bản trong hệ thống. Kết quả chỉ mang tính hỗ trợ và không thay thế chẩn đoán y khoa.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <span className="inline-flex items-center gap-2 rounded-full bg-emerald-200 px-4 py-2 text-emerald-800">
              <BadgeCheck className="size-5" />Dữ liệu từ hệ thống
            </span>
            <span className="inline-flex items-center gap-2 rounded-full bg-sky-200 px-4 py-2 text-brand-700">
              <LockKeyhole className="size-5" />Bảo mật
            </span>
          </div>
        </div>
        <div className="h-[285px] overflow-hidden rounded-3xl border border-line bg-white shadow-xl">
          <img className="size-full object-cover opacity-25" src="/assets/mindcare-wellness-illustration.png" alt="" />
        </div>
      </section>

      <section>
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-medium">Bài đánh giá hiện có</h2>
          <Link className="flex items-center gap-2 text-brand-700" to="/assessments/library">
            Xem tất cả <ArrowRight className="size-5" />
          </Link>
        </div>
        {assessments.isLoading ? <Loading /> : (
          <div className="grid gap-6 lg:grid-cols-3">
            {assessments.data?.slice(0, 3).map((item) => <AssessmentCard key={item.id} assessment={item} />)}
          </div>
        )}
      </section>

      <section>
        <h2 className="border-b border-line px-6 py-4 font-semibold text-brand-700">Lịch sử làm bài</h2>
        <Card className="mt-6 overflow-hidden">
          <div className="hidden grid-cols-[1.25fr_1fr_1fr_1fr] bg-slate-100 px-8 py-5 text-sm font-semibold md:grid">
            <span>Bài đánh giá</span><span>Ngày hoàn thành</span><span>Kết quả</span><span>Hành động</span>
          </div>
          {history.isLoading && <Loading />}
          {history.data?.items.map((item) => (
            <div className="grid gap-3 border-t border-line px-8 py-5 md:grid-cols-[1.25fr_1fr_1fr_1fr] md:items-center" key={item.resultId}>
              <b>{item.assessmentCode}</b>
              <span className="text-slate-500">{new Date(item.createdAt).toLocaleString("vi-VN")}</span>
              <Badge className="w-fit" tone={item.riskLevel === "NORMAL" ? "success" : "warning"}>{item.riskLevel}</Badge>
              <Link className="text-brand-700" to={`/assessments/${item.assessmentCode}/result?resultId=${item.resultId}`}>Xem chi tiết ↗</Link>
            </div>
          ))}
          {!history.isLoading && history.data?.items.length === 0 && (
            <p className="px-8 py-10 text-center text-muted">Bạn chưa có kết quả đánh giá nào.</p>
          )}
        </Card>
      </section>
    </div>
  );
}

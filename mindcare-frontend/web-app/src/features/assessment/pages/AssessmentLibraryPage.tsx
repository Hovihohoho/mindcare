import { ShieldCheck } from "lucide-react";
import { Loading, PageHeader } from "@/shared";
import { AssessmentCard } from "../components/AssessmentCard";
import { useAssessments } from "../hooks/useAssessments";

export function AssessmentLibraryPage() {
  const assessments = useAssessments();
  return (
    <div className="space-y-10">
      <PageHeader eyebrow="Đánh giá tâm lý" title="Thư viện đánh giá tâm lý" description="Các công cụ sàng lọc khoa học giúp bạn hiểu rõ hơn về trạng thái tinh thần hiện tại. Kết quả không thay thế chẩn đoán y khoa." />
      {assessments.isLoading ? <Loading /> : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {assessments.data?.map((assessment) => <AssessmentCard key={assessment.code} assessment={assessment} />)}
        </div>
      )}
      <div className="flex gap-4 rounded-2xl bg-brand-900 p-6 text-white md:p-8">
        <ShieldCheck className="size-8 shrink-0 text-brand-100" />
        <div><h3 className="font-extrabold">Nguồn và giới hạn được công khai</h3><p className="mt-2 max-w-3xl text-sm leading-6 text-white/70">Mỗi bài đánh giá hiển thị tổ chức phát hành, tài liệu gốc, phiên bản và giới hạn sử dụng trước khi bạn trả lời.</p></div>
      </div>
    </div>
  );
}

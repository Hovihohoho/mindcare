import { ArrowRight, BadgeCheck, LockKeyhole } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Card } from "@/shared";
import { AssessmentCard } from "../components/AssessmentCard";
import { assessmentMocks } from "../constants/assessment.mock";

const history = [
  { title: "Lo âu tổng quát (GAD-7)", date: "15 Tháng 10, 2024", level: "MỨC ĐỘ NHẸ", tone: "warning" as const },
  { title: "Trầm cảm (PHQ-9)", date: "02 Tháng 09, 2024", level: "BÌNH THƯỜNG", tone: "success" as const },
  { title: "Mức độ Stress", date: "15 Tháng 08, 2024", level: "TRUNG BÌNH", tone: "warning" as const },
];

export function AssessmentOverviewPage() {
  return (
    <div className="space-y-16">
      <section className="grid items-center gap-10 lg:grid-cols-[1fr_1.05fr]">
        <div>
          <h1 className="text-4xl font-bold leading-tight text-brand-700 md:text-5xl">Đánh giá tâm lý</h1>
          <p className="mt-4 max-w-[630px] text-lg leading-7 text-slate-500">Hãy dành ít phút để hiểu rõ hơn về tình trạng sức khỏe tinh thần của bạn thông qua các bài kiểm tra được thiết kế bởi các chuyên gia tâm lý hàng đầu. Kết quả của bạn được bảo mật hoàn toàn.</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <span className="inline-flex items-center gap-2 rounded-full bg-emerald-200 px-4 py-2 text-emerald-800"><BadgeCheck className="size-5" />Tiêu chuẩn y khoa</span>
            <span className="inline-flex items-center gap-2 rounded-full bg-sky-200 px-4 py-2 text-brand-700"><LockKeyhole className="size-5" />Bảo mật 100%</span>
          </div>
        </div>
        <div className="h-[285px] overflow-hidden rounded-3xl border border-line bg-white shadow-xl">
          <img className="size-full object-cover opacity-25" src="/assets/mindcare-wellness-illustration.png" alt="" />
        </div>
      </section>
      <section>
        <div className="mb-6 flex items-center justify-between"><h2 className="text-lg font-medium">Bài kiểm tra đề xuất</h2><Link className="flex items-center gap-2 text-brand-700" to="/assessments/library">Xem tất cả <ArrowRight className="size-5" /></Link></div>
        <div className="grid gap-6 lg:grid-cols-3">{assessmentMocks.slice(0, 3).map((item) => <AssessmentCard key={item.code} assessment={item} />)}</div>
      </section>
      <section>
        <div className="flex gap-12 border-b border-line"><button className="border-b-2 border-brand-700 px-6 py-4 font-semibold text-brand-700">Lịch sử làm bài</button><button className="px-2 py-4 text-slate-500">Kết quả lưu trữ</button></div>
        <Card className="mt-6 overflow-hidden">
          <div className="hidden grid-cols-[1.25fr_1fr_1fr_1fr] bg-slate-100 px-8 py-5 text-sm font-semibold md:grid"><span>Tên bài kiểm tra</span><span>Ngày hoàn thành</span><span>Kết quả</span><span>Hành động</span></div>
          {history.map((item) => <div className="grid gap-3 border-t border-line px-8 py-5 md:grid-cols-[1.25fr_1fr_1fr_1fr] md:items-center" key={item.title}><b>{item.title}</b><span className="text-slate-500">{item.date}</span><Badge className="w-fit" tone={item.tone}>{item.level}</Badge><button className="text-left text-brand-700">Xem chi tiết ↗</button></div>)}
        </Card>
      </section>
    </div>
  );
}

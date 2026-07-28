import { Link, useLocation } from "react-router-dom";
import { Badge, Button, Card, PageHeader } from "@/shared";
import type { AssessmentResult } from "../types/assessment.types";

export function AssessmentResultPage() {
  const result = (useLocation().state as { result?: AssessmentResult } | null)?.result;

  if (!result) {
    return (
      <Card className="mx-auto max-w-xl p-8 text-center">
        <h1 className="text-2xl font-extrabold">Không tìm thấy kết quả trong phiên này</h1>
        <p className="mt-3 text-muted">Bạn có thể xem lịch sử kết quả tại trang đánh giá.</p>
        <Link to="/assessments"><Button className="mt-5">Về trang đánh giá</Button></Link>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Hoàn thành đánh giá" title={`Kết quả ${result.assessmentCode}`} description={result.screeningNotice} />
      <div className="grid gap-6 lg:grid-cols-[.8fr_1.2fr]">
        <Card className="flex flex-col items-center justify-center p-8 text-center">
          <p className="text-sm font-semibold text-muted">Tổng điểm · phiên bản {result.assessmentVersion}</p>
          <p className="mt-2 text-6xl font-black text-brand-900">{result.totalScore}</p>
          <Badge tone={result.riskLevel === "NORMAL" ? "success" : "warning"} className="mt-4">{result.riskLevel}</Badge>
          <p className="mt-5 text-xs text-muted">{new Date(result.createdAt).toLocaleString("vi-VN")}</p>
        </Card>
        <Card className="p-6 md:p-8">
          <h2 className="text-xl font-extrabold">Khuyến nghị</h2>
          <div className="mt-5 space-y-3">
            {result.recommendations.map((recommendation) => <p className="rounded-xl bg-slate-50 p-4 text-sm leading-6" key={recommendation}>{recommendation}</p>)}
          </div>
          <p className="mt-5 text-sm text-muted">Kết quả chỉ mang tính sàng lọc, không phải chẩn đoán y khoa.</p>
        </Card>
      </div>
      <div className="flex justify-end gap-3"><Link to="/assessments"><Button variant="outline">Về trang đánh giá</Button></Link><Link to="/experts"><Button>Trao đổi với chuyên gia</Button></Link></div>
    </div>
  );
}

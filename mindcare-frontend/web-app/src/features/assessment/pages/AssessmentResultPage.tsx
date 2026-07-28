import { useQuery } from "@tanstack/react-query";
import { Sparkles } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { Badge, Button, Card, Loading, PageHeader } from "@/shared";
import { assessmentApi } from "../api/assessment.api";

export function AssessmentResultPage() {
  const [params] = useSearchParams();
  const resultId = params.get("resultId") ?? "";
  const result = useQuery({
    queryKey: ["assessment-result", resultId],
    queryFn: () => assessmentApi.result(resultId),
    enabled: Boolean(resultId),
  });

  if (result.isLoading) return <Loading />;
  if (!result.data) {
    return <p className="rounded-xl bg-rose-50 p-4 text-rose-700">Không tìm thấy kết quả đánh giá.</p>;
  }

  const item = result.data;
  const tone = item.riskLevel === "NORMAL" ? "success" : "warning";

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Hoàn thành đánh giá"
        title="Kết quả bài đánh giá"
        description={`Kết quả ${item.assessmentCode}, phiên bản ${item.assessmentVersion}.`}
      />
      <div className="grid gap-6 lg:grid-cols-[.8fr_1.2fr]">
        <Card className="flex flex-col items-center justify-center p-8 text-center">
          <span className="grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-600">
            <Sparkles className="size-8" />
          </span>
          <p className="mt-5 text-sm font-semibold text-muted">Tổng điểm</p>
          <p className="mt-2 text-6xl font-black text-brand-900">{item.totalScore}</p>
          <Badge tone={tone} className="mt-4">{item.riskLevel}</Badge>
          <p className="mt-6 text-sm leading-6 text-muted">{item.screeningNotice}</p>
        </Card>
        <Card className="p-6 md:p-8">
          <h2 className="text-xl font-extrabold">Khuyến nghị</h2>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            {item.recommendations.map((text, index) => (
              <div className="rounded-xl bg-slate-50 p-4" key={text}>
                <Sparkles className="size-5 text-brand-600" />
                <h3 className="mt-3 font-bold">Gợi ý {index + 1}</h3>
                <p className="mt-2 text-sm leading-6 text-muted">{text}</p>
              </div>
            ))}
          </div>
        </Card>
      </div>
      <div className="flex flex-wrap justify-end gap-3">
        <Link to="/assessments"><Button variant="outline">Về thư viện</Button></Link>
        <Link to="/experts"><Button>Trao đổi với chuyên gia</Button></Link>
      </div>
    </div>
  );
}

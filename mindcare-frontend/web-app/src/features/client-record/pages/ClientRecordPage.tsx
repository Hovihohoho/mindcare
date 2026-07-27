import { ArrowLeft, BriefcaseBusiness, MapPin, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Avatar, Badge, Button, Card } from "@/shared";
import { EmotionTrendChart } from "@/features/emotion";
import { ClientAssessmentSummary } from "../components/ClientAssessmentSummary";

export function ClientRecordPage() {
  const navigate = useNavigate();
  return (
    <div className="mx-auto max-w-[1180px] space-y-8">
      <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100" onClick={() => navigate(-1)} aria-label="Quay lại"><ArrowLeft /></button>
      <Card className="flex flex-col gap-6 p-7 md:flex-row md:items-center">
        <Avatar className="size-32 rounded-3xl text-2xl shadow-lg" fallback="TM" />
        <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-3"><h1 className="text-3xl font-medium">Nguyễn Thị Mai</h1><Badge tone="success">Đang theo dõi tích cực</Badge></div><div className="mt-4 flex flex-wrap gap-5 text-slate-600"><span className="flex items-center gap-2"><BriefcaseBusiness className="size-5" />24 tuổi</span><span className="flex items-center gap-2"><UserRound className="size-5" />Nữ</span><span className="flex items-center gap-2"><BriefcaseBusiness className="size-5" />Nhân viên văn phòng</span></div><p className="mt-4 flex items-center gap-2 text-slate-600"><MapPin className="size-5" />Quận 7, TP. HCM</p></div>
        <div className="flex gap-3"><Button variant="outline">Gửi tin nhắn</Button><Button>Tạo ghi chú</Button></div>
      </Card>
      <ClientAssessmentSummary />
      <section><h2 className="border-b border-line px-6 py-4 text-lg font-medium text-blue-600">Lịch sử cảm xúc</h2><Card className="mt-6 p-8"><div className="flex flex-wrap justify-between gap-4"><h2 className="text-2xl font-semibold">Xu hướng cảm xúc trong tuần</h2><div className="flex gap-3"><span className="rounded-full bg-slate-100 px-4 py-2 text-brand-700">7 ngày qua</span><span className="px-4 py-2 text-brand-700">tháng trước</span></div></div><EmotionTrendChart /></Card></section>
    </div>
  );
}

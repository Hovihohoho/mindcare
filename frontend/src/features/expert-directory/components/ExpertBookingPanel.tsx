import { ArrowRight, ShieldCheck, Video } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

export function ExpertBookingPanel({ expert }: { expert: ExpertSummary }) {
  return (
    <aside className="space-y-5 lg:sticky lg:top-28">
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Đặt lịch tham vấn</h2>
        <p className="mt-5 text-sm font-semibold">Chọn hình thức</p>
        <button className="mt-2 flex items-center gap-2 rounded-lg border-2 border-brand-700 px-6 py-3 font-semibold text-brand-700"><Video className="size-5" />Online</button>
        <p className="mt-5 text-sm text-muted">Chọn ngày và khung giờ đang khả dụng ở bước tiếp theo. Dữ liệu lịch được tải trực tiếp từ hệ thống.</p>
        <p className="mt-5 text-2xl font-bold text-brand-700">{Number(expert.consultationFee).toLocaleString("vi-VN")} {expert.currency}</p>
        <Link className="mt-7 block" to={`/booking/${expert.expertUserId}`}><Button className="w-full bg-blue-400 hover:bg-blue-500" size="lg">Chọn lịch <ArrowRight className="size-5" /></Button></Link>
        <p className="mt-3 text-center text-xs text-muted">Bạn sẽ chưa bị trừ phí ở bước này.</p>
      </Card>
      <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800"><ShieldCheck className="size-5 shrink-0" />Bảo mật thông tin 100%. Mọi cuộc hội thoại đều được mã hóa đầu cuối.</div>
    </aside>
  );
}

import { ArrowRight, ShieldCheck, Video } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

export function ExpertBookingPanel({ expert }: { expert: ExpertSummary }) {
  return (
    <aside className="space-y-5 lg:sticky lg:top-28">
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Đặt lịch tham vấn</h2>
        <div className="mt-5 flex items-center gap-3 rounded-xl border border-line p-4">
          <Video className="text-brand-700" />
          <div>
            <p className="font-semibold">Tham vấn trực tuyến</p>
            <p className="mt-1 text-sm text-muted">{Number(expert.consultationFee).toLocaleString("vi-VN")} {expert.currency}</p>
          </div>
        </div>
        <p className="mt-5 text-sm text-muted">Khung giờ khả dụng sẽ được tải từ Booking Service ở bước tiếp theo.</p>
        <Link className="mt-7 block" to={`/booking/${expert.expertUserId}`}>
          <Button className="w-full bg-blue-400 hover:bg-blue-500" size="lg">Chọn khung giờ <ArrowRight className="size-5" /></Button>
        </Link>
      </Card>
      <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800">
        <ShieldCheck className="size-5 shrink-0" />Dữ liệu lịch được lấy trực tiếp từ hệ thống.
      </div>
    </aside>
  );
}

import { MessageCircle, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

export function ExpertBookingPanel({ expert }: { expert: ExpertSummary }) {
  return (
    <aside className="space-y-5 lg:sticky lg:top-28">
      <Card className="p-6">
        <h2 className="text-2xl font-semibold">Trao đổi với chuyên gia</h2>
        <div className="mt-5 flex items-center gap-3 rounded-xl border border-line p-4">
          <MessageCircle className="text-brand-700" />
          <div>
            <p className="font-semibold">Chat trực tiếp</p>
            <p className="mt-1 text-sm text-muted">Không cần đặt lịch hoặc thanh toán</p>
          </div>
        </div>
        <p className="mt-5 text-sm text-muted">Bạn có thể chủ động mở cuộc trò chuyện và xem lại lịch sử bất kỳ lúc nào.</p>
        <Link className="mt-7 block" to={`/chat?expertId=${expert.expertUserId}`}>
          <Button className="w-full bg-blue-400 hover:bg-blue-500" size="lg">Bắt đầu trò chuyện <MessageCircle className="size-5" /></Button>
        </Link>
      </Card>
      <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-800">
        <ShieldCheck className="size-5 shrink-0" />Nội dung chat chỉ hiển thị cho hai người tham gia.
      </div>
    </aside>
  );
}

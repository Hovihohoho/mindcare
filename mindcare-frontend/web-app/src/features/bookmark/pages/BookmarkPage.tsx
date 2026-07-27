import { Bookmark, Clock3, Star, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { Avatar, Badge, Button, Card, PageHeader } from "@/shared";

export function BookmarkPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Nội dung đã lưu" description="Tất cả bài đánh giá và chuyên gia bạn quan tâm ở cùng một nơi." />
      <section><h2 className="mb-4 flex items-center gap-2 text-lg font-extrabold"><Bookmark className="size-5 text-brand-600" />Bài đánh giá</h2><div className="grid gap-4 md:grid-cols-2"><Card className="p-5"><div className="flex justify-between"><Badge>Lo âu</Badge><button aria-label="Xóa"><Trash2 className="size-4 text-slate-400" /></button></div><h3 className="mt-4 text-lg font-extrabold">Lo âu tổng quát (GAD-7)</h3><p className="mt-2 text-sm leading-6 text-muted">Xác định mức độ lo lắng trong cuộc sống hằng ngày qua 7 câu hỏi khoa học.</p><div className="mt-4 flex items-center justify-between"><span className="flex items-center gap-1 text-xs text-muted"><Clock3 className="size-4" />5 phút</span><Link to="/assessments/GAD_7"><Button size="sm">Bắt đầu làm bài</Button></Link></div></Card></div></section>
      <section><h2 className="mb-4 text-lg font-extrabold">Chuyên gia</h2><div className="grid gap-4 md:grid-cols-2"><Card className="flex items-center gap-4 p-5"><Avatar className="size-16" fallback="NH" /><div className="min-w-0 flex-1"><h3 className="font-extrabold">TS. Nguyễn Thu Hà</h3><p className="truncate text-sm text-brand-600">Chuyên gia Tâm lý học Lâm sàng</p><p className="mt-2 flex items-center gap-1 text-xs"><Star className="size-4 fill-amber-400 text-amber-400" />4.9 · 12 năm kinh nghiệm</p></div><Link to="/experts/10000000-0000-0000-0000-000000000001"><Button size="sm">Đặt lịch</Button></Link></Card></div></section>
    </div>
  );
}

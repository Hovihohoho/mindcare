import { BedDouble, BookHeart, Brain, Footprints, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, PageHeader } from "@/shared";

const advice = [
  { icon: Brain, title: "Thực hành Mindfulness", text: "Dành 5–10 phút mỗi ngày để thiền định hoặc hít thở sâu." },
  { icon: BedDouble, title: "Cải thiện giấc ngủ", text: "Đi ngủ trước 23 giờ và hạn chế thiết bị điện tử trước khi ngủ." },
  { icon: Footprints, title: "Vận động nhẹ nhàng", text: "Đi dạo ngoài trời ít nhất 15 phút mỗi ngày." },
  { icon: BookHeart, title: "Ghi chép cảm xúc", text: "Theo dõi những thay đổi nhỏ trong tâm trạng bằng Nhật ký." },
];

export function AssessmentResultPage() {
  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Hoàn thành đánh giá" title="Kết quả bài đánh giá" description="Cảm ơn bạn đã tin tưởng MindCare. Đây là phân tích tham khảo dựa trên câu trả lời của bạn." />
      <div className="grid gap-6 lg:grid-cols-[.8fr_1.2fr]">
        <Card className="flex flex-col items-center justify-center p-8 text-center">
          <span className="grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-600"><Sparkles className="size-8" /></span>
          <p className="mt-5 text-sm font-semibold text-muted">Chỉ số tâm lý tổng quát</p>
          <p className="mt-2 text-6xl font-black text-brand-900">13<span className="text-xl text-muted">/21</span></p>
          <Badge tone="warning" className="mt-4">MỨC ĐỘ: NHẸ</Badge>
          <div className="mt-7 grid w-full grid-cols-3 gap-3 text-sm"><div><b className="text-emerald-600">72%</b><p className="text-muted">Hạnh phúc</p></div><div><b className="text-amber-600">38%</b><p className="text-muted">Lo âu</p></div><div><b className="text-violet-600">45%</b><p className="text-muted">Căng thẳng</p></div></div>
        </Card>
        <Card className="p-6 md:p-8">
          <h2 className="text-xl font-extrabold">Lời khuyên hữu ích</h2>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            {advice.map(({ icon: Icon, title, text }) => <div className="rounded-xl bg-slate-50 p-4" key={title}><Icon className="size-5 text-brand-600" /><h3 className="mt-3 font-bold">{title}</h3><p className="mt-2 text-sm leading-6 text-muted">{text}</p></div>)}
          </div>
        </Card>
      </div>
      <div className="flex flex-wrap justify-end gap-3"><Link to="/assessments"><Button variant="outline">Về thư viện</Button></Link><Link to="/experts"><Button>Trao đổi với chuyên gia</Button></Link></div>
    </div>
  );
}

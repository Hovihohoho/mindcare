import { Award, MessageSquareText, UserRound, Video } from "lucide-react";
import { useParams } from "react-router-dom";
import { Card, Loading } from "@/shared";
import { ExpertBookingPanel } from "../components/ExpertBookingPanel";
import { ExpertProfileHeader } from "../components/ExpertProfileHeader";
import { useExpert } from "../hooks/useExperts";

export function ExpertProfilePage() {
  const { id = "" } = useParams();
  const expert = useExpert(id);
  if (!expert.data || expert.isLoading) return <Loading />;
  const item = expert.data;
  return (
    <div>
      <p className="mb-6 text-sm text-slate-500">Chuyên gia <span className="mx-2">›</span><b className="text-slate-700">{item.displayName}</b></p>
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_385px]">
        <div className="space-y-6">
          <ExpertProfileHeader expert={item} />
          <Card className="p-7"><h2 className="flex items-center gap-3 text-2xl font-semibold"><UserRound className="text-brand-700" />Giới thiệu</h2><p className="mt-5 leading-8 text-slate-500">{item.bio ?? "Chuyên gia giàu kinh nghiệm, luôn lắng nghe và đồng hành cùng bạn bằng phương pháp thực hành dựa trên bằng chứng."}</p></Card>
          <Card className="p-7"><h2 className="flex items-center gap-3 text-2xl font-semibold"><Award className="text-brand-700" />Bằng cấp & Chuyên môn</h2><div className="mt-6 space-y-5">{(item.education ?? ["Thạc sĩ Tâm lý học", "Chứng chỉ tham vấn chuyên sâu"]).map((value) => <div className="flex gap-3" key={value}><span className="grid size-10 shrink-0 place-items-center rounded-lg bg-slate-100 text-brand-700"><Award className="size-5" /></span><div><b>{value}</b><p className="mt-1 text-sm text-muted">Đã được MindCare xác thực</p></div></div>)}</div></Card>
          <Card className="p-7"><h2 className="flex items-center gap-3 text-2xl font-semibold"><Video className="text-brand-700" />Dịch vụ & Phí tham vấn</h2><div className="mt-5 flex items-center justify-between rounded-xl border border-line p-6"><div><Video className="text-brand-700" /><b className="mt-4 block">Tham vấn Online</b><p className="mt-2 max-w-md text-sm text-muted">Phù hợp cho khách hàng ở xa, hỗ trợ qua Google Meet hoặc Zoom với tính bảo mật cao.</p></div><p className="text-2xl font-bold text-brand-700">{item.consultationFee / 1000}k <small className="text-sm font-normal text-muted">/60p</small></p></div></Card>
          <Card className="p-7"><div className="flex justify-between"><h2 className="flex items-center gap-3 text-2xl font-semibold"><MessageSquareText className="text-brand-700" />Phản hồi từ người dùng</h2><button className="text-sm font-semibold text-brand-700">Xem tất cả</button></div><div className="mt-6 divide-y divide-line">{["Minh T.", "Hoàng L."].map((name) => <div className="py-5" key={name}><b>{name}</b><p className="mt-1 text-amber-400">★★★★★</p><p className="mt-2 text-slate-500">Chuyên gia rất lắng nghe và thấu hiểu. Tôi cảm thấy bình tĩnh hơn khi đối diện với áp lực công việc.</p></div>)}</div></Card>
        </div>
        <ExpertBookingPanel expert={item} />
      </div>
    </div>
  );
}

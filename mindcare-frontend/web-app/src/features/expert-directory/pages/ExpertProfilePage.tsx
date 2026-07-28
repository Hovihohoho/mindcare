import { Award, Video } from "lucide-react";
import { useParams } from "react-router-dom";
import { Card, Loading } from "@/shared";
import { ExpertBookingPanel } from "../components/ExpertBookingPanel";
import { ExpertProfileHeader } from "../components/ExpertProfileHeader";
import { useExpert } from "../hooks/useExperts";

export function ExpertProfilePage() {
  const { id = "" } = useParams();
  const expert = useExpert(id);
  if (expert.isLoading) return <Loading />;
  if (!expert.data) return <p className="rounded-xl bg-rose-50 p-4 text-rose-700">Không tìm thấy chuyên gia.</p>;
  const item = expert.data;

  return (
    <div>
      <p className="mb-6 text-sm text-slate-500">Chuyên gia <span className="mx-2">›</span><b className="text-slate-700">{item.displayName}</b></p>
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_385px]">
        <div className="space-y-6">
          <ExpertProfileHeader expert={item} />
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><Award className="text-brand-700" />Thông tin chuyên môn</h2>
            <dl className="mt-6 grid gap-5 sm:grid-cols-2">
              <div><dt className="text-sm text-muted">Tiêu đề</dt><dd className="mt-1 font-semibold">{item.headline}</dd></div>
              <div><dt className="text-sm text-muted">Kinh nghiệm</dt><dd className="mt-1 font-semibold">{item.yearsOfExperience} năm</dd></div>
              <div className="sm:col-span-2"><dt className="text-sm text-muted">Chuyên môn</dt><dd className="mt-2 flex flex-wrap gap-2">{item.specialties.map((value) => <span className="rounded-full bg-slate-100 px-3 py-1 text-sm" key={value}>{value}</span>)}</dd></div>
            </dl>
          </Card>
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><Video className="text-brand-700" />Phí tham vấn</h2>
            <p className="mt-5 text-3xl font-bold text-brand-700">{Number(item.consultationFee).toLocaleString("vi-VN")} {item.currency}</p>
          </Card>
          <Card className="p-7 text-sm text-muted">
            Booking Service hiện chỉ trả dữ liệu tóm tắt chuyên gia; tiểu sử, bằng cấp chi tiết và nội dung đánh giá sẽ chỉ hiển thị khi backend có API tương ứng.
          </Card>
        </div>
        <ExpertBookingPanel expert={item} />
      </div>
    </div>
  );
}

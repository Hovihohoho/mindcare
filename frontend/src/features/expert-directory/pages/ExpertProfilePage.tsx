import { Award, UserRound, Video } from "lucide-react";
import { useParams } from "react-router-dom";
import { Card, EmptyState, Loading } from "@/shared";
import { ExpertBookingPanel } from "../components/ExpertBookingPanel";
import { ExpertProfileHeader } from "../components/ExpertProfileHeader";
import { useExpert } from "../hooks/useExperts";

export function ExpertProfilePage() {
  const { id = "" } = useParams();
  const expert = useExpert(id);
  if (expert.isLoading) return <Loading />;
  if (!expert.data || expert.isError) return <EmptyState title="Không tìm thấy chuyên gia" description="Hồ sơ này không tồn tại hoặc chưa được công khai." />;
  const item = expert.data;
  return (
    <div>
      <p className="mb-6 text-sm text-slate-500">Chuyên gia <span className="mx-2">›</span><b className="text-slate-700">{item.displayName}</b></p>
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_385px]">
        <div className="space-y-6">
          <ExpertProfileHeader expert={item} />
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><UserRound className="text-brand-700" />Giới thiệu</h2>
            <p className="mt-5 leading-8 text-slate-500">{item.bio || "Chuyên gia chưa cập nhật phần giới thiệu."}</p>
          </Card>
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><Award className="text-brand-700" />Bằng cấp & chuyên môn</h2>
            {item.education?.length ? <ul className="mt-5 list-disc space-y-2 pl-5">{item.education.map((value) => <li key={value}>{value}</li>)}</ul> : <p className="mt-5 text-slate-500">Chưa có dữ liệu học vấn công khai.</p>}
          </Card>
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><Video className="text-brand-700" />Phí tham vấn</h2>
            <p className="mt-5 text-2xl font-bold text-brand-700">{item.consultationFee.toLocaleString("vi-VN")} {item.currency}<small className="ml-2 text-sm font-normal text-muted">/ phiên</small></p>
          </Card>
        </div>
        <ExpertBookingPanel expert={item} />
      </div>
    </div>
  );
}

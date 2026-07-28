import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Card } from "@/shared";

export function ClientRecordPage() {
  const navigate = useNavigate();
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100" onClick={() => navigate(-1)} aria-label="Quay lại"><ArrowLeft /></button>
      <Card className="p-10 text-center">
        <LockKeyhole className="mx-auto size-12 text-brand-600" />
        <h1 className="mt-5 text-2xl font-extrabold">Dữ liệu thân chủ được bảo vệ</h1>
        <p className="mx-auto mt-3 max-w-xl leading-7 text-muted">MindCare chưa mở API chia sẻ assessment và nhật ký cho chuyên gia vì contract consent/authorization vẫn chưa được phê duyệt. Trang này không hiển thị dữ liệu mẫu để tránh tạo cảm giác rằng chuyên gia có thể xem dữ liệu khi chưa có sự đồng ý.</p>
      </Card>
    </div>
  );
}

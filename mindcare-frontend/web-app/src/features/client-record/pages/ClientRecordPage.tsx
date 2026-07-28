import { ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Card } from "@/shared";

export function ClientRecordPage() {
  const navigate = useNavigate();
  return (
    <div className="mx-auto max-w-[900px] space-y-8">
      <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100" onClick={() => navigate(-1)} aria-label="Quay lại">
        <ArrowLeft />
      </button>
      <Card className="p-10 text-center">
        <h1 className="text-3xl font-bold text-brand-700">Hồ sơ khách hàng</h1>
        <p className="mx-auto mt-4 max-w-2xl leading-7 text-muted">
          Backend hiện chưa cung cấp API consent để chuyên gia đọc kết quả đánh giá hoặc nhật ký cảm xúc của người dùng.
          MindCare không hiển thị dữ liệu giả lập cho màn hình này.
        </p>
      </Card>
    </div>
  );
}

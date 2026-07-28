import { FileWarning } from "lucide-react";
import { EmptyState, PageHeader } from "@/shared";

export function ExpertRegistrationPage() {
  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Trở thành chuyên gia MindCare" title="Đăng ký hồ sơ chuyên gia" description="Quy trình xét duyệt chuyên gia đang được hoàn thiện." />
      <EmptyState
        title="Chưa thể gửi hồ sơ"
        description="Backend chưa có API tiếp nhận hồ sơ và tài liệu xác minh. Biểu mẫu không hoạt động trước đây đã được tạm khóa để tránh làm mất dữ liệu người dùng nhập."
        action={<FileWarning className="mx-auto text-amber-600" />}
      />
    </div>
  );
}

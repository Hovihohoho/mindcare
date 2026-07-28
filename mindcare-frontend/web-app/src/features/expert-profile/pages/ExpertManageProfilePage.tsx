import { Card, PageHeader } from "@/shared";
import { useCurrentUser } from "@/features/auth";

export function ExpertManageProfilePage() {
  const user = useCurrentUser();
  return (
    <div className="space-y-8">
      <PageHeader title="Hồ sơ chuyên gia" description="Thông tin tài khoản từ Auth Service." />
      <Card className="p-8">
        <p><span className="text-muted">Họ tên:</span> <b>{user.data?.fullName ?? "Đang tải..."}</b></p>
        <p className="mt-3"><span className="text-muted">Email:</span> <b>{user.data?.email ?? "Đang tải..."}</b></p>
        <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-muted">Backend chưa có schema/API hồ sơ chuyên môn, chứng chỉ hoặc đơn vị công tác. Các dữ liệu giả lập đã được loại bỏ.</p>
      </Card>
    </div>
  );
}

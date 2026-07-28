import { useQuery } from "@tanstack/react-query";
import { BriefcaseBusiness, Mail } from "lucide-react";
import { authApi } from "@/features/auth";
import { Avatar, Badge, Card, EmptyState, Loading } from "@/shared";

export function ExpertManageProfilePage() {
  const profile = useQuery({ queryKey: ["auth-me"], queryFn: authApi.me });
  if (profile.isLoading) return <Loading />;
  if (!profile.data || profile.isError) return <p className="text-sm text-rose-600">Không thể tải tài khoản chuyên gia.</p>;
  return (
    <div className="mx-auto max-w-[900px] space-y-6">
      <header><h1 className="text-3xl font-bold">Hồ sơ chuyên gia</h1><p className="mt-2 text-slate-600">Thông tin tài khoản đang được hệ thống xác thực.</p></header>
      <Card className="flex flex-col gap-5 p-7 sm:flex-row sm:items-center">
        <Avatar className="size-24 text-2xl" fallback={profile.data.fullName} />
        <div><h2 className="text-2xl font-bold">{profile.data.fullName}</h2><p className="mt-2 flex items-center gap-2 text-muted"><Mail className="size-4" />{profile.data.email}</p><Badge className="mt-3" tone={profile.data.active ? "success" : "warning"}>{profile.data.active ? "Đang hoạt động" : "Đã vô hiệu hóa"}</Badge></div>
      </Card>
      <EmptyState title="Chưa thể chỉnh sửa hồ sơ chuyên môn" description="Backend chưa cung cấp API đọc/cập nhật hồ sơ chuyên môn, chứng chỉ và học vấn. Màn hình dữ liệu mẫu đã được thay thế để không gây nhầm lẫn." action={<BriefcaseBusiness className="mx-auto text-brand-700" />} />
    </div>
  );
}

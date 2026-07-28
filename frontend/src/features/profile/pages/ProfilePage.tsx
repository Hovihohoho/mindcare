import { useQuery } from "@tanstack/react-query";
import { Mail, ShieldCheck, UserRound } from "lucide-react";
import { authApi } from "@/features/auth";
import { Avatar, Badge, Card, Loading, PageHeader } from "@/shared";

export function ProfilePage() {
  const profile = useQuery({ queryKey: ["auth-me"], queryFn: authApi.me });
  if (profile.isLoading) return <Loading />;
  if (!profile.data || profile.isError) return <p className="text-sm text-rose-600">Không thể tải thông tin tài khoản.</p>;
  const user = profile.data;
  return (
    <div className="space-y-7">
      <PageHeader title="Hồ sơ tài khoản" description="Thông tin được lấy trực tiếp từ tài khoản MindCare của bạn." />
      <Card className="p-7">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
          <Avatar className="size-28 text-2xl" fallback={user.fullName} />
          <div><h1 className="text-2xl font-bold">{user.fullName}</h1><p className="mt-2 flex items-center gap-2 text-muted"><Mail className="size-4" />{user.email}</p><div className="mt-4 flex flex-wrap gap-2"><Badge>{user.role.replace("ROLE_", "")}</Badge><Badge tone={user.emailVerified ? "success" : "warning"}>{user.emailVerified ? "Email đã xác minh" : "Email chưa xác minh"}</Badge></div></div>
        </div>
      </Card>
      <Card className="grid gap-5 p-7 sm:grid-cols-2">
        <div><p className="text-sm text-muted">Mã tài khoản</p><p className="mt-2 break-all font-medium">{user.id}</p></div>
        <div><p className="text-sm text-muted">Ngày tạo</p><p className="mt-2 font-medium">{new Date(user.createdAt).toLocaleString("vi-VN")}</p></div>
        <div className="flex items-center gap-3 sm:col-span-2"><ShieldCheck className="text-brand-700" /><div><p className="font-semibold">Trạng thái</p><p className="text-sm text-muted">{user.active ? "Tài khoản đang hoạt động" : "Tài khoản đã bị vô hiệu hóa"}</p></div></div>
      </Card>
      <Card className="flex items-start gap-3 border-blue-100 bg-blue-50 p-6"><UserRound className="mt-0.5 text-brand-700" /><p className="text-sm text-slate-600">Backend hiện chưa cung cấp API cập nhật hồ sơ cá nhân. Chức năng chỉnh sửa sẽ được bật khi API này sẵn sàng.</p></Card>
    </div>
  );
}

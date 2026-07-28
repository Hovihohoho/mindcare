import { UserRound } from "lucide-react";
import { Avatar, Badge, Card, Loading } from "@/shared";
import { useCurrentUser } from "@/features/auth";

export function ProfilePage() {
  const user = useCurrentUser();
  if (user.isLoading) return <Loading />;
  if (!user.data) return <p className="rounded-xl bg-rose-50 p-4 text-rose-700">Không thể tải thông tin tài khoản.</p>;

  const item = user.data;
  return (
    <div className="space-y-8">
      <Card className="p-7">
        <div className="flex flex-col gap-7 md:flex-row md:items-center">
          <Avatar className="size-32 border-4 border-sky-200 text-2xl" fallback={item.fullName} />
          <div className="flex-1">
            <h1 className="text-2xl font-semibold">{item.fullName}</h1>
            <p className="mt-2 text-slate-500">{item.email}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Badge tone="neutral">{item.role}</Badge>
              <Badge tone={item.emailVerified ? "success" : "warning"}>{item.emailVerified ? "Email đã xác thực" : "Email chưa xác thực"}</Badge>
              <Badge tone={item.active ? "success" : "warning"}>{item.active ? "Đang hoạt động" : "Đã khóa"}</Badge>
            </div>
          </div>
        </div>
      </Card>
      <Card className="p-8">
        <h2 className="flex items-center gap-3 text-xl font-medium"><UserRound className="text-slate-700" />Thông tin tài khoản</h2>
        <dl className="mt-6 grid gap-5 sm:grid-cols-2">
          <div><dt className="text-sm text-muted">Mã người dùng</dt><dd className="mt-1 break-all font-medium">{item.id}</dd></div>
          <div><dt className="text-sm text-muted">Ngày tạo</dt><dd className="mt-1 font-medium">{new Date(item.createdAt).toLocaleString("vi-VN")}</dd></div>
          <div><dt className="text-sm text-muted">Họ tên</dt><dd className="mt-1 font-medium">{item.fullName}</dd></div>
          <div><dt className="text-sm text-muted">Email</dt><dd className="mt-1 font-medium">{item.email}</dd></div>
        </dl>
        <p className="mt-8 rounded-xl bg-slate-50 p-4 text-sm text-muted">Auth Service hiện chưa cung cấp API cập nhật hồ sơ, nên màn hình chỉ hiển thị dữ liệu thật và không giả lập thao tác lưu.</p>
      </Card>
    </div>
  );
}

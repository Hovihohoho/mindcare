import { useQuery } from "@tanstack/react-query";
import { Card, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminDashboardPage() {
  const stats = useQuery({ queryKey: ["admin-dashboard"], queryFn: adminApi.dashboard });
  if (stats.isLoading) return <Loading />;
  const cards = [["Tổng người dùng", stats.data?.totalUsers], ["Đang hoạt động", stats.data?.activeUsers], ["Chuyên gia", stats.data?.experts], ["Chờ duyệt", stats.data?.pendingExperts]];
  return <div className="space-y-7"><PageHeader title="Dashboard quản trị" description="Số liệu trực tiếp từ hệ thống." /><div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label, value]) => <Card className="p-6" key={label}><p className="text-sm text-muted">{label}</p><p className="mt-2 text-3xl font-bold">{value ?? 0}</p></Card>)}</div></div>;
}

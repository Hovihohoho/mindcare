import { useQuery } from "@tanstack/react-query";
import { BellRing, BookOpenText, ShieldCheck, UsersRound } from "lucide-react";
import { Card, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminDashboardPage() {
  const users = useQuery({ queryKey: ["admin", "users"], queryFn: adminApi.users });
  const documents = useQuery({ queryKey: ["admin", "documents"], queryFn: adminApi.documents });
  if (users.isLoading || documents.isLoading) return <Loading />;
  const active = users.data?.filter((item) => item.active).length ?? 0;
  const experts = users.data?.filter((item) => item.role === "ROLE_EXPERT").length ?? 0;
  const cards = [
    ["Tổng tài khoản", users.data?.length ?? 0, UsersRound, "bg-blue-100 text-blue-700"],
    ["Đang hoạt động", active, ShieldCheck, "bg-emerald-100 text-emerald-700"],
    ["Chuyên gia", experts, BellRing, "bg-violet-100 text-violet-700"],
    ["Tài liệu AI", documents.data?.length ?? 0, BookOpenText, "bg-amber-100 text-amber-700"],
  ] as const;
  return <div className="space-y-7"><PageHeader eyebrow="MindCare Admin" title="Tổng quan hệ thống" description="Theo dõi nhanh tài khoản và kho dữ liệu AI." /><div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label, value, Icon, tone]) => <Card className="p-6" key={label}><span className={`grid size-12 place-items-center rounded-xl ${tone}`}><Icon /></span><p className="mt-5 text-sm text-muted">{label}</p><p className="mt-1 text-3xl font-bold">{value}</p></Card>)}</div></div>;
}

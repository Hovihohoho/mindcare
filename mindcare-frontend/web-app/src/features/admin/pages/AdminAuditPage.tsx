import { useQuery } from "@tanstack/react-query";
import { Card, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminAuditPage() {
  const logs = useQuery({ queryKey: ["admin-audit"], queryFn: () => adminApi.audit() });
  if (logs.isLoading) return <Loading />;
  return <div className="space-y-6"><PageHeader title="Nhật ký thao tác admin" description="Theo dõi các thay đổi quan trọng." /><Card className="divide-y p-4">{logs.data?.content.map((log) => <div className="py-4" key={log.id}><div className="flex justify-between gap-4"><b>{log.action}</b><span className="text-sm text-muted">{new Date(log.createdAt).toLocaleString("vi-VN")}</span></div><p className="text-sm text-muted">{log.adminEmail} · {log.targetType} {log.targetId}</p>{log.detail && <p className="mt-1 text-sm">{log.detail}</p>}</div>)}</Card></div>;
}

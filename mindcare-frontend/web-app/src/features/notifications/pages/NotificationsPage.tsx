import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, ExternalLink } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, EmptyState, Loading, PageHeader } from "@/shared";
import { notificationApi } from "../api/notification.api";

export function NotificationsPage() {
  const client = useQueryClient();
  const notifications = useQuery({ queryKey: ["notifications"], queryFn: notificationApi.list });
  const markRead = useMutation({
    mutationFn: notificationApi.markRead,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["notifications"] });
      client.invalidateQueries({ queryKey: ["notifications", "unread"] });
    },
  });
  return (
    <div className="page-container space-y-7 py-10">
      <PageHeader title="Thông báo" description="Cập nhật mới nhất từ MindCare." />
      {notifications.isLoading ? <Loading /> : notifications.data?.length ? (
        <div className="space-y-3">{notifications.data.map((item) => (
          <Card className={`p-5 ${item.read ? "bg-white" : "border-brand-200 bg-brand-50/50"}`} key={item.id}>
            <button className="w-full text-left" onClick={() => !item.read && markRead.mutate(item.id)}>
              <div className="flex gap-4"><span className="grid size-10 shrink-0 place-items-center rounded-full bg-white text-brand-700"><Bell className="size-5" /></span><div className="min-w-0 flex-1"><div className="flex justify-between gap-3"><h2 className="font-bold">{item.title}</h2><time className="shrink-0 text-xs text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</time></div><p className="mt-2 text-sm leading-6 text-slate-600">{item.content}</p>{item.actionUrl && <Link className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-brand-700" to={item.actionUrl}>Xem chi tiết <ExternalLink className="size-3" /></Link>}</div></div>
            </button>
          </Card>
        ))}</div>
      ) : <EmptyState title="Chưa có thông báo" description="Thông báo từ MindCare sẽ xuất hiện tại đây." />}
    </div>
  );
}

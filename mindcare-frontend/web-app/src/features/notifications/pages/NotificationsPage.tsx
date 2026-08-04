import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Button, Card, EmptyState, getApiErrorMessage, Loading, PageHeader } from "@/shared";
import { notificationApi } from "../api/notification.api";

export function NotificationsPage() {
  const navigate = useNavigate();
  const [actionError, setActionError] = useState<string | null>(null);
  const notifications = useQuery({ queryKey: ["notifications"], queryFn: notificationApi.list });
  if (notifications.isLoading) return <Loading />;

  async function open(id: string, actionUrl?: string) {
    setActionError(null);
    try {
      await notificationApi.markRead(id);
      await notifications.refetch();
      if (actionUrl?.startsWith("/") && !actionUrl.startsWith("//")) navigate(actionUrl);
    } catch (error) {
      setActionError(getApiErrorMessage(error, "Không thể cập nhật thông báo."));
    }
  }

  async function markAllRead() {
    setActionError(null);
    try {
      await notificationApi.markAllRead();
      await notifications.refetch();
    } catch (error) {
      setActionError(getApiErrorMessage(error, "Không thể đánh dấu thông báo."));
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Thông báo" description="Các cập nhật về tài khoản, hồ sơ và hoạt động của bạn." />
      {actionError && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{actionError}</p>}
      {!!notifications.data?.some((item) => !item.readAt) && (
        <Button onClick={() => { void markAllRead(); }} variant="outline">
          Đánh dấu tất cả đã đọc
        </Button>
      )}
      {!notifications.data?.length && <EmptyState title="Bạn chưa có thông báo" />}
      <div className="space-y-3">
        {notifications.data?.map((item) => (
          <Card className={item.readAt ? "p-5 opacity-70" : "border-brand-200 bg-brand-50/40 p-5"} key={item.id}>
            <button className="w-full text-left" onClick={() => { void open(item.id, item.actionUrl); }}>
              <div className="flex items-start justify-between gap-3">
                <h2 className="font-bold">{item.title}</h2>
                {!item.readAt && <span className="mt-1 size-2 shrink-0 rounded-full bg-brand-600" />}
              </div>
              <p className="mt-2 whitespace-pre-wrap text-sm text-muted">{item.content}</p>
              <time className="mt-3 block text-xs text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</time>
            </button>
          </Card>
        ))}
      </div>
    </div>
  );
}

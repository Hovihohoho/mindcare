import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, BellOff, CheckCheck, ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { cn } from "@/shared";
import { notificationApi } from "../api/notification.api";
import type { NotificationPage } from "../types/notification.types";

export function NotificationPopover() {
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const root = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const notifications = useQuery({
    queryKey: ["notifications", page],
    queryFn: () => notificationApi.list(page),
    refetchInterval: 60_000,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["notifications"] });
  const markRead = useMutation({ mutationFn: notificationApi.markRead, onSuccess: refresh });
  const markAll = useMutation({
    mutationFn: notificationApi.markAllRead,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["notifications"] });
      const previous = queryClient.getQueriesData<NotificationPage>({ queryKey: ["notifications"] });
      const readAt = new Date().toISOString();
      previous.forEach(([queryKey, data]) => {
        if (!data) return;
        queryClient.setQueryData<NotificationPage>(queryKey, {
          ...data,
          unreadCount: 0,
          items: data.items.map((item) => item.readAt ? item : { ...item, readAt }),
        });
      });
      return { previous };
    },
    onError: (_error, _variables, context) => {
      context?.previous.forEach(([queryKey, data]) => queryClient.setQueryData(queryKey, data));
    },
    onSettled: refresh,
  });

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const unread = notifications.data?.unreadCount ?? 0;
  const togglePopover = () => {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    if (!markAll.isPending) markAll.mutate();
  };

  return (
    <div className="relative" ref={root}>
      <button className="focus-ring relative rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label={`Thông báo, ${unread} chưa đọc`} aria-expanded={open} onClick={togglePopover}>
        <Bell className="size-5" />
        {unread > 0 && <span className="absolute right-0 top-0 grid min-w-5 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">{Math.min(unread, 99)}</span>}
      </button>
      {open && (
        <section className="fixed inset-x-3 top-20 z-50 overflow-hidden rounded-2xl border border-line bg-white shadow-2xl sm:absolute sm:-right-14 sm:top-12 sm:left-auto sm:w-[390px]" aria-label="Danh sách thông báo">
          <header className="flex items-center justify-between border-b border-line px-4 py-3">
            <div><h2 className="font-bold text-slate-800">Thông báo</h2><p className="text-xs text-muted">{unread} thông báo chưa đọc</p></div>
            <button className="flex items-center gap-1 text-xs font-semibold text-brand-700 disabled:opacity-40" disabled={!unread || markAll.isPending} onClick={() => markAll.mutate()}><CheckCheck className="size-4" />Đọc tất cả</button>
          </header>
          <div className="max-h-[min(60vh,520px)] overflow-y-auto">
            {notifications.isLoading && <p className="p-8 text-center text-sm text-muted">Đang tải...</p>}
            {!notifications.isLoading && notifications.data?.items.length === 0 && <div className="p-8 text-center text-muted"><BellOff className="mx-auto mb-2 size-8" /><p>Chưa có thông báo.</p></div>}
            {notifications.data?.items.map((item) => (
              <button key={item.id} className={cn("block w-full border-b border-line px-4 py-3 text-left transition hover:bg-slate-50", !item.readAt && "bg-cyan-50/70")} onClick={() => {
                if (!item.readAt) markRead.mutate(item.id);
                setOpen(false);
                if (item.actionUrl) navigate(item.actionUrl);
              }}>
                <div className="flex gap-3"><span className={cn("mt-1 size-2 shrink-0 rounded-full", item.readAt ? "bg-transparent" : "bg-brand-600")} /><div><p className="text-sm font-bold text-slate-800">{item.title}</p><p className="mt-1 text-sm leading-5 text-slate-600">{item.message}</p><time className="mt-1 block text-[11px] text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</time></div></div>
              </button>
            ))}
          </div>
          {(notifications.data?.totalPages ?? 0) > 1 && <footer className="flex items-center justify-between px-4 py-2 text-xs"><button disabled={page === 0} onClick={() => setPage((value) => value - 1)} aria-label="Trang trước"><ChevronLeft className="size-4" /></button><span>Trang {page + 1}/{notifications.data?.totalPages}</span><button disabled={page + 1 >= (notifications.data?.totalPages ?? 0)} onClick={() => setPage((value) => value + 1)} aria-label="Trang sau"><ChevronRight className="size-4" /></button></footer>}
        </section>
      )}
    </div>
  );
}

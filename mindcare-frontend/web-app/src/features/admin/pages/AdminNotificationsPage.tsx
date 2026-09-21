import { useState } from "react";
import { Button, Card, getApiErrorMessage, Input, PageHeader, Textarea } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminNotificationsPage() {
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null);
  const [sending, setSending] = useState(false);

  async function send(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setSending(true);
    setMessage(null);
    try {
      const count = await adminApi.broadcast(
        String(data.get("title")),
        String(data.get("content")),
        String(data.get("actionUrl") || "") || undefined,
      );
      form.reset();
      setMessage({ text: `Đã gửi tới ${count} người dùng.`, error: false });
    } catch (error) {
      setMessage({ text: getApiErrorMessage(error, "Không thể gửi thông báo."), error: true });
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Thông báo toàn hệ thống" description="Gửi thông báo tới tất cả tài khoản đang hoạt động." />
      <Card className="p-7">
        <form className="grid gap-4" onSubmit={send}>
          <Input label="Tiêu đề" name="title" required />
          <Textarea label="Nội dung" name="content" required rows={6} />
          <Input label="Đường dẫn hành động" name="actionUrl" placeholder="/notifications" />
          <Button className="w-fit" loading={sending}>Gửi thông báo</Button>
          {message && <p role="status" className={message.error ? "text-rose-700" : "text-emerald-700"}>{message.text}</p>}
        </form>
      </Card>
    </div>
  );
}

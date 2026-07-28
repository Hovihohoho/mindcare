import { useState, type FormEvent } from "react";
import { BellRing, Send } from "lucide-react";
import { Button, Card, Input, PageHeader, Textarea } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminNotificationsPage() {
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSending(true);
    setResult("");
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    try {
      const count = await adminApi.broadcast({ title: String(values.title), content: String(values.content), actionUrl: String(values.actionUrl) || null });
      setResult(`Đã gửi thông báo đến ${count} tài khoản đang hoạt động.`);
      form.reset();
    } finally {
      setSending(false);
    }
  }
  return <div className="space-y-7"><PageHeader title="Gửi thông báo" description="Phát thông báo hệ thống đến tất cả người dùng đang hoạt động." /><Card className="max-w-3xl p-7"><h2 className="flex items-center gap-2 text-xl font-bold"><BellRing className="text-brand-700" />Nội dung thông báo</h2><form className="mt-6 space-y-5" onSubmit={submit}><Input label="Tiêu đề" maxLength={255} name="title" required /><Textarea label="Nội dung" maxLength={5000} name="content" required rows={6} /><Input hint="Ví dụ: /assessments hoặc /experts" label="Đường dẫn khi nhấn thông báo" maxLength={500} name="actionUrl" />{result && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">{result}</p>}<Button leftIcon={<Send className="size-4" />} loading={sending} type="submit">Gửi đến người dùng</Button></form></Card></div>;
}

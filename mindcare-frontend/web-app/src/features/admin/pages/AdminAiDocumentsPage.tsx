import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { Button, Card, EmptyState, getApiErrorMessage, Input, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminAiDocumentsPage() {
  const [message, setMessage] = useState("");
  const docs = useQuery({ queryKey: ["admin-ai-documents"], queryFn: adminApi.documents });
  const upload = useMutation({
    mutationFn: ({ file, title, sourceUrl }: { file: File; title?: string; sourceUrl: string }) => adminApi.uploadDocument(file, title, sourceUrl),
    onSuccess: () => { setMessage("Đã tải tài liệu. Hãy kiểm tra nguồn rồi cho phép AI sử dụng."); void docs.refetch(); },
    onError: () => setMessage("Không thể tải tài liệu. Tệp phải là TXT, PDF hoặc DOCX và có đường dẫn HTTPS."),
  });

  async function run(action: () => Promise<void>, success: string) {
    setMessage("");
    try { await action(); await docs.refetch(); setMessage(success); }
    catch (error) { setMessage(getApiErrorMessage(error, "Không thể thực hiện thao tác với tài liệu.")); }
  }

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const file = data.get("file");
    if (!(file instanceof File) || !file.size) return setMessage("Vui lòng chọn tài liệu.");
    upload.mutate({ file, title: String(data.get("title") || "") || undefined, sourceUrl: String(data.get("sourceUrl") || "") }, { onSuccess: () => form.reset() });
  }

  if (docs.isLoading) return <Loading />;
  return <div className="space-y-7">
    <PageHeader title="Nguồn kiến thức của AI" description="Mỗi tài liệu cần có đường dẫn chứng minh. Hãy mở nguồn để kiểm tra trước khi cho phép AI sử dụng." />
    {message && <p className="rounded-xl bg-brand-50 p-3 text-sm text-brand-800">{message}</p>}
    <Card className="p-6">
      <h2 className="text-xl font-bold">Thêm tài liệu</h2>
      <form className="mt-5 grid gap-4 md:grid-cols-2" onSubmit={submit}>
        <Input accept=".txt,.pdf,.docx" label="Tệp tài liệu" name="file" type="file" required />
        <Input label="Tên tài liệu" name="title" />
        <Input className="md:col-span-2" label="Đường dẫn nguồn chứng minh" name="sourceUrl" type="url" required />
        <Button className="w-fit" loading={upload.isPending}>Tải tài liệu</Button>
      </form>
    </Card>
    {!docs.data?.length && <EmptyState title="Chưa có tài liệu" />}
    <div className="space-y-3">{docs.data?.map((doc) => {
      const enabled = doc.reviewStatus === "APPROVED" && doc.processingStatus === "READY";
      return <Card className="p-5" key={doc.id}>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="font-bold">{doc.title}</h2>
              <span className={enabled ? "rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-700" : "rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700"}>{enabled ? "Được AI sử dụng" : "Chưa xác nhận"}</span>
            </div>
            {doc.publisher && <p className="mt-2 text-sm text-muted">Nguồn: {doc.publisher}</p>}
            <a className="mt-2 inline-flex items-center gap-1 break-all text-sm font-semibold text-brand-700 underline" href={doc.sourceUrl} rel="noreferrer" target="_blank">Mở nguồn chứng minh<ExternalLink className="size-4 shrink-0" /></a>
            {doc.processingError && <p className="mt-2 text-sm text-rose-700">{doc.processingError}</p>}
            <details className="mt-3"><summary className="cursor-pointer text-sm font-semibold text-brand-700">Xem nội dung tài liệu</summary><p className="mt-2 max-h-64 overflow-auto whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm">{doc.content}</p></details>
          </div>
          <div className="flex flex-wrap gap-2">
            {!enabled ? <Button onClick={() => { void run(() => adminApi.enableDocument(doc.id), "Đã cho phép AI sử dụng tài liệu."); }}>Cho phép AI sử dụng</Button> : <Button onClick={() => { void run(() => adminApi.disableDocument(doc.id), "Đã ngừng sử dụng tài liệu."); }} variant="outline">Ngừng sử dụng</Button>}
            <Button onClick={() => { if (window.confirm(`Xóa tài liệu “${doc.title}”?`)) void run(() => adminApi.deleteDocument(doc.id), "Đã xóa tài liệu."); }} variant="outline">Xóa</Button>
          </div>
        </div>
      </Card>;
    })}</div>
  </div>;
}

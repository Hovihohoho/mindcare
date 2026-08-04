import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button, Card, EmptyState, getApiErrorMessage, Input, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

const statusLabel = {
  PROCESSING: "Đang xử lý",
  READY: "Sẵn sàng",
  FAILED: "Xử lý lỗi",
} as const;

export function AdminAiDocumentsPage() {
  const [message, setMessage] = useState("");
  const docs = useQuery({ queryKey: ["admin-ai-documents"], queryFn: adminApi.documents });
  const upload = useMutation({
    mutationFn: ({ file, title, sourceUrl }: { file: File; title?: string; sourceUrl?: string }) =>
      adminApi.uploadDocument(file, title, sourceUrl),
    onSuccess: (document) => {
      setMessage(document.processingStatus === "FAILED"
        ? "Đã đọc tài liệu nhưng chưa thể lập chỉ mục. Bạn có thể thử lại sau."
        : "Đã tải và lập chỉ mục tài liệu.");
      docs.refetch();
    },
    onError: () => setMessage("Không thể tải tài liệu. Chỉ nhận TXT, PDF hoặc DOCX tối đa 15MB."),
  });

  async function runDocumentAction(action: () => Promise<void>, success: string) {
    setMessage("");
    try {
      await action();
      await docs.refetch();
      setMessage(success);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "Không thể thực hiện thao tác với tài liệu AI."));
    }
  }

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const file = data.get("file");
    if (!(file instanceof File) || !file.size) {
      setMessage("Vui lòng chọn tài liệu.");
      return;
    }
    upload.mutate({
      file,
      title: String(data.get("title") || "") || undefined,
      sourceUrl: String(data.get("sourceUrl") || "") || undefined,
    }, { onSuccess: () => form.reset() });
  }

  if (docs.isLoading) return <Loading />;
  return (
    <div className="space-y-7">
      <PageHeader title="Tài liệu AI" description="Tải tài liệu kiến thức để AI trích xuất nội dung và lập chỉ mục tìm kiếm." />
      {message && <p className="rounded-xl bg-brand-50 p-3 text-sm text-brand-800">{message}</p>}
      <Card className="p-6">
        <h2 className="text-xl font-bold">Tải tài liệu mới</h2>
        <p className="mt-1 text-sm text-muted">Hỗ trợ TXT, PDF và DOCX; tối đa 15MB. PDF dạng ảnh cần OCR trước khi tải.</p>
        <form className="mt-5 grid gap-4 md:grid-cols-2" onSubmit={submit}>
          <Input accept=".txt,.pdf,.docx" label="Tệp tài liệu" name="file" type="file" required />
          <Input label="Tiêu đề (không bắt buộc)" name="title" />
          <Input className="md:col-span-2" label="URL nguồn (không bắt buộc)" name="sourceUrl" type="url" />
          <Button className="w-fit" loading={upload.isPending}>Tải lên và lập chỉ mục</Button>
        </form>
      </Card>
      {!docs.data?.length && <EmptyState title="Chưa có tài liệu AI" />}
      <div className="space-y-3">
        {docs.data?.map((doc) => {
          const status = doc.processingStatus ?? "READY";
          return (
            <Card className="p-5" key={doc.id}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <h2 className="font-bold">{doc.title}</h2>
                  <p className="mt-1 text-sm text-muted">
                    {doc.originalFilename || doc.documentType || "KNOWLEDGE"}
                    {doc.fileSize ? ` · ${(doc.fileSize / 1024).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} KB` : ""}
                    {doc.indexedAt ? ` · Lập chỉ mục ${new Date(doc.indexedAt).toLocaleString("vi-VN")}` : ""}
                  </p>
                  <span className={status === "READY" ? "mt-2 inline-block text-sm font-semibold text-emerald-700" : status === "FAILED" ? "mt-2 inline-block text-sm font-semibold text-rose-700" : "mt-2 inline-block text-sm font-semibold text-amber-700"}>
                    {statusLabel[status]}
                  </span>
                  {doc.processingError && <p className="mt-2 text-sm text-rose-600">{doc.processingError}</p>}
                  <details className="mt-3">
                    <summary className="cursor-pointer text-sm font-semibold text-brand-700">Xem nội dung đã trích xuất</summary>
                    <p className="mt-2 max-h-64 overflow-auto whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm">{doc.content}</p>
                  </details>
                </div>
                <div className="flex gap-2">
                  <Button onClick={() => { void runDocumentAction(() => adminApi.reindexDocument(doc.id), "Đã lập chỉ mục lại tài liệu."); }} variant="outline">Lập chỉ mục lại</Button>
                  <Button onClick={() => {
                    if (window.confirm(`Xóa tài liệu “${doc.title}”?`)) {
                      void runDocumentAction(() => adminApi.deleteDocument(doc.id), "Đã xóa tài liệu.");
                    }
                  }} variant="outline">Xóa</Button>
                </div>
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}

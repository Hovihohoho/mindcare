import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, getApiErrorMessage, Input, Loading, PageHeader, Textarea } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminContentPage() {
  const [documentMessage, setDocumentMessage] = useState("");
  const [creating, setCreating] = useState(false);
  const docs = useQuery({ queryKey: ["admin-ai-documents"], queryFn: adminApi.documents });
  const assessments = useQuery({ queryKey: ["admin-assessments"], queryFn: adminApi.assessments });

  async function runAction(action: () => Promise<void>, refresh: () => Promise<unknown>, success: string) {
    setDocumentMessage("");
    try {
      await action();
      await refresh();
      setDocumentMessage(success);
    } catch (error) {
      setDocumentMessage(getApiErrorMessage(error, "Không thể thực hiện thao tác."));
    }
  }

  async function create(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setCreating(true);
    setDocumentMessage("");
    try {
      const document = await adminApi.createDocument({
        title: String(data.get("title")),
        content: String(data.get("content")),
        sourceUrl: String(data.get("sourceUrl")),
        documentType: String(data.get("documentType") || "SELF_CARE_ARTICLE"),
        active: true,
      });
      form.reset();
      await docs.refetch();
      setDocumentMessage(document.processingStatus === "FAILED"
        ? "Đã lưu tài liệu nhưng chưa thể lập chỉ mục. Hãy kiểm tra GEMINI_API_KEY rồi thử lại."
        : document.processingStatus === "PENDING_REVIEW"
          ? "Đã lưu nội dung và chuyển sang bước chờ kiểm duyệt."
          : "Đã thêm và lập chỉ mục tài liệu AI.");
    } catch {
      setDocumentMessage("Không thể thêm tài liệu AI. Vui lòng kiểm tra AI Service và dữ liệu nhập.");
    } finally {
      setCreating(false);
    }
  }

  if (docs.isLoading || assessments.isLoading) return <Loading />;
  return (
    <div className="space-y-8">
      <PageHeader title="Nội dung tâm lý và tài liệu AI" description="Quản lý bài đánh giá và dữ liệu kiến thức của AI." />
      <Card className="p-6">
        <h2 className="text-xl font-bold">Bài đánh giá tâm lý</h2>
        <div className="mt-4 divide-y">
          {assessments.data?.map((item) => (
            <div className="flex items-center justify-between py-4" key={item.id}>
              <div><b>{item.title}</b><p className="text-sm text-muted">{item.code} · v{item.version} · {item.status}</p></div>
              <div className="flex gap-2">
                <Button onClick={() => { void runAction(() => adminApi.publishAssessment(item.id), () => assessments.refetch(), "Đã xuất bản bài đánh giá."); }} variant="outline">Xuất bản</Button>
                <Button onClick={() => { void runAction(() => adminApi.archiveAssessment(item.id), () => assessments.refetch(), "Đã lưu trữ bài đánh giá."); }} variant="outline">Lưu trữ</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
      <Card className="p-6">
        <h2 className="text-xl font-bold">Nhập nhanh tài liệu AI</h2>
        <p className="mt-1 text-sm text-muted">Bạn cũng có thể tải TXT, PDF hoặc DOCX tại mục “Tài liệu AI” trên menu.</p>
        {documentMessage && <p className="mt-3 rounded-lg bg-brand-50 p-3 text-sm text-brand-800">{documentMessage}</p>}
        <form className="mt-4 grid gap-4" onSubmit={create}>
          <Input label="Tiêu đề" name="title" required />
          <Input label="Nguồn" name="sourceUrl" type="url" />
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Loại nội dung
            <select className="h-12 rounded-xl border border-line bg-white px-4" defaultValue="SELF_CARE_ARTICLE" name="documentType">
              <option value="SELF_CARE_ARTICLE">Bài viết tự chăm sóc</option>
              <option value="KNOWLEDGE">Tài liệu kiến thức cho AI</option>
            </select>
          </label>
          <Textarea label="Nội dung" name="content" required rows={6} />
          <Button className="w-fit" loading={creating}>Thêm và lập chỉ mục</Button>
        </form>
        <div className="mt-6 divide-y">
          {docs.data?.map((doc) => (
            <div className="flex items-center justify-between gap-4 py-4" key={doc.id}>
              <div>
                <b>{doc.title}</b>
                <p className="text-sm text-muted">{doc.documentType} · {doc.processingStatus ?? "READY"} · {doc.active ? "Đang dùng" : "Tắt"}</p>
                {doc.processingError && <p className="mt-1 text-sm text-rose-600">{doc.processingError}</p>}
              </div>
              <div className="flex gap-2">
                <Button onClick={() => { void runAction(() => adminApi.reindexDocument(doc.id), () => docs.refetch(), "Đã lập chỉ mục lại tài liệu."); }} variant="outline">Lập chỉ mục lại</Button>
                <Button onClick={() => {
                  if (window.confirm(`Xóa tài liệu “${doc.title}”?`)) {
                    void runAction(() => adminApi.deleteDocument(doc.id), () => docs.refetch(), "Đã xóa tài liệu.");
                  }
                }} variant="outline">Xóa</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

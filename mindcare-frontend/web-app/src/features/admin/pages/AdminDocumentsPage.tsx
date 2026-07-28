import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { FileUp, RefreshCw } from "lucide-react";
import { Button, Card, Input, PageHeader, Textarea } from "@/shared";
import { adminApi } from "../api/admin.api";
import type { KnowledgeDocument } from "../types/admin.types";

export function AdminDocumentsPage() {
  const client = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);
  const documents = useQuery({ queryKey: ["admin", "documents"], queryFn: adminApi.documents });
  const [editing, setEditing] = useState<KnowledgeDocument | null>(null);
  const [open, setOpen] = useState(false);
  const [imported, setImported] = useState<{ title: string; content: string } | null>(null);
  const refresh = () => client.invalidateQueries({ queryKey: ["admin", "documents"] });
  const remove = useMutation({ mutationFn: adminApi.deleteDocument, onSuccess: refresh });
  const reindex = useMutation({ mutationFn: adminApi.reindexDocument, onSuccess: refresh });
  const reindexAll = useMutation({ mutationFn: adminApi.reindexAll, onSuccess: refresh });

  async function importFile(file?: File) {
    if (!file) return;
    if (!["text/plain", "text/markdown", ""].includes(file.type) || file.size > 2_000_000) {
      window.alert("Chỉ hỗ trợ file .txt hoặc .md tối đa 2 MB.");
      return;
    }
    setImported({ title: file.name.replace(/\.(txt|md)$/i, ""), content: await file.text() });
    setEditing(null);
    setOpen(true);
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    const payload = { title: String(values.title), content: String(values.content), sourceUrl: String(values.sourceUrl) || null, documentType: String(values.documentType), active: values.active === "on" };
    if (editing) await adminApi.updateDocument(editing.id, payload);
    else await adminApi.createDocument(payload);
    setEditing(null);
    setImported(null);
    setOpen(false);
    await refresh();
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Kho dữ liệu AI" description="Nạp nội dung đã kiểm duyệt và tạo embedding cho MindCare AI." actions={<div className="flex flex-wrap gap-2"><input accept=".txt,.md,text/plain,text/markdown" className="hidden" ref={fileInput} type="file" onChange={(event) => importFile(event.target.files?.[0])} /><Button leftIcon={<FileUp className="size-4" />} onClick={() => fileInput.current?.click()} variant="outline">Nhập TXT/MD</Button><Button leftIcon={<RefreshCw className="size-4" />} loading={reindexAll.isPending} onClick={() => reindexAll.mutate()} variant="outline">Reindex tất cả</Button><Button onClick={() => { setEditing(null); setImported(null); setOpen(true); }}>Thêm tài liệu</Button></div>} />
      <Card className="border-blue-100 bg-blue-50 p-5 text-sm text-slate-700"><b>Cách nạp dữ liệu:</b> nhập trực tiếp hoặc chọn file TXT/Markdown, kiểm tra tiêu đề và nguồn, sau đó lưu. AI Service sẽ tự tạo embedding bằng Gemini. Chỉ bật tài liệu đã được kiểm duyệt và không chứa dữ liệu cá nhân nhạy cảm.</Card>
      {open && <Card className="p-6"><form className="space-y-4" onSubmit={submit}><Input defaultValue={editing?.title ?? imported?.title ?? ""} label="Tiêu đề" maxLength={255} name="title" required /><Textarea defaultValue={editing?.content ?? imported?.content ?? ""} label="Nội dung" name="content" required rows={12} /><div className="grid gap-4 md:grid-cols-2"><Input defaultValue={editing?.sourceUrl ?? ""} label="Nguồn tham khảo" name="sourceUrl" type="url" /><label className="text-sm font-semibold">Loại tài liệu<select className="mt-2 h-12 w-full rounded-xl border border-line px-3" defaultValue={editing?.documentType ?? (imported ? "MARKDOWN" : "TEXT")} name="documentType"><option value="TEXT">Văn bản</option><option value="MARKDOWN">Markdown</option><option value="GUIDELINE">Hướng dẫn chuyên môn</option><option value="FAQ">FAQ</option></select></label></div><label className="flex items-center gap-2"><input defaultChecked={editing?.active ?? true} name="active" type="checkbox" />Cho phép AI sử dụng</label><div className="flex gap-3"><Button>Lưu và tạo embedding</Button><Button onClick={() => setOpen(false)} type="button" variant="outline">Hủy</Button></div></form></Card>}
      <div className="grid gap-4">{documents.data?.map((document) => <Card className="p-5" key={document.id}><div className="flex flex-wrap justify-between gap-4"><div className="min-w-0 flex-1"><div className="flex gap-2"><h2 className="font-bold">{document.title}</h2><span className={`rounded-full px-2 py-1 text-xs ${document.active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{document.active ? "Đang dùng" : "Tạm ẩn"}</span></div><p className="mt-2 line-clamp-2 text-sm text-muted">{document.content}</p><p className="mt-2 text-xs text-muted">{document.documentType} · {new Date(document.updatedAt).toLocaleString("vi-VN")}</p></div><div className="flex gap-2"><Button size="sm" variant="outline" onClick={() => { setEditing(document); setImported(null); setOpen(true); }}>Sửa</Button><Button size="sm" variant="outline" loading={reindex.isPending} onClick={() => reindex.mutate(document.id)}>Reindex</Button><Button size="sm" variant="ghost" loading={remove.isPending} onClick={() => window.confirm("Xóa tài liệu này?") && remove.mutate(document.id)}>Xóa</Button></div></div></Card>)}</div>
      {documents.isError && <p className="text-rose-700">Không tải được tài liệu AI. Hãy kiểm tra AI Service và GEMINI_API_KEY.</p>}
    </div>
  );
}

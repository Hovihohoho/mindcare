import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button, Card, Input, PageHeader, Textarea } from "@/shared";
import { adminApi } from "../api/admin.api";
import type { KnowledgeDocument } from "../types/admin.types";

export function AdminDocumentsPage() {
  const queryClient = useQueryClient();
  const documents = useQuery({ queryKey: ["admin", "documents"], queryFn: adminApi.documents });
  const [editing, setEditing] = useState<KnowledgeDocument | null>(null);
  const [open, setOpen] = useState(false);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "documents"] });
  const remove = useMutation({ mutationFn: adminApi.deleteDocument, onSuccess: refresh });
  const reindex = useMutation({ mutationFn: adminApi.reindexDocument, onSuccess: refresh });

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    const payload = { title: String(values.title), content: String(values.content), sourceUrl: String(values.sourceUrl) || null, documentType: "TEXT", active: values.active === "on" };
    if (editing) await adminApi.updateDocument(editing.id, payload);
    else await adminApi.createDocument(payload);
    setEditing(null);
    setOpen(false);
    await refresh();
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Tài liệu RAG" description="Quản lý nội dung đã kiểm duyệt dùng cho MindCare AI." actions={<Button onClick={() => { setEditing(null); setOpen(true); }}>Thêm tài liệu</Button>} />
      {open && <Card className="p-6"><form className="space-y-4" onSubmit={submit}><Input name="title" label="Tiêu đề" maxLength={255} required defaultValue={editing?.title} /><Textarea name="content" label="Nội dung" rows={10} required defaultValue={editing?.content} /><Input name="sourceUrl" label="Nguồn tham khảo" defaultValue={editing?.sourceUrl ?? ""} /><label className="flex items-center gap-2"><input name="active" type="checkbox" defaultChecked={editing?.active ?? true} />Đang sử dụng</label><div className="flex gap-3"><Button>Lưu và tạo embedding</Button><Button type="button" variant="outline" onClick={() => setOpen(false)}>Hủy</Button></div></form></Card>}
      <div className="grid gap-4">{documents.data?.map((document) => <Card className="p-5" key={document.id}><div className="flex flex-wrap justify-between gap-4"><div className="min-w-0"><h2 className="font-extrabold">{document.title}</h2><p className="mt-2 line-clamp-2 text-sm text-muted">{document.content}</p><p className="mt-2 text-xs text-muted">{document.active ? "Đang dùng" : "Tạm ẩn"} · {new Date(document.updatedAt).toLocaleString("vi-VN")}</p></div><div className="flex gap-2"><Button size="sm" variant="outline" onClick={() => { setEditing(document); setOpen(true); }}>Sửa</Button><Button size="sm" variant="outline" loading={reindex.isPending} onClick={() => reindex.mutate(document.id)}>Reindex</Button><Button size="sm" variant="ghost" loading={remove.isPending} onClick={() => window.confirm("Xóa tài liệu này?") && remove.mutate(document.id)}>Xóa</Button></div></div></Card>)}</div>
      {documents.isError && <p className="text-rose-700">Không tải được tài liệu AI.</p>}
    </div>
  );
}

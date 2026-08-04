import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, EmptyState, getApiErrorMessage, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

export function AdminExpertsPage() {
  const [status, setStatus] = useState("PENDING");
  const [selectedId, setSelectedId] = useState<string>();
  const [actionMessage, setActionMessage] = useState<{ text: string; error: boolean } | null>(null);
  const experts = useQuery({
    queryKey: ["admin-experts", status],
    queryFn: () => adminApi.experts(status),
  });
  const detail = useQuery({
    queryKey: ["admin-expert", selectedId],
    queryFn: () => adminApi.expert(selectedId!),
    enabled: Boolean(selectedId),
  });

  async function review(id: string, decision: "APPROVED" | "REJECTED", reason: string) {
    setActionMessage(null);
    try {
      await adminApi.reviewExpert(id, decision, reason);
      await experts.refetch();
      setSelectedId(undefined);
      setActionMessage({ text: decision === "APPROVED" ? "Đã duyệt hồ sơ chuyên gia." : "Đã từ chối hồ sơ chuyên gia.", error: false });
    } catch (error) {
      setActionMessage({ text: getApiErrorMessage(error, "Không thể cập nhật hồ sơ chuyên gia."), error: true });
    }
  }

  async function openDocument(id: string) {
    setActionMessage(null);
    try {
      await adminApi.openExpertDocument(id);
    } catch (error) {
      setActionMessage({ text: getApiErrorMessage(error, "Không thể mở tài liệu minh chứng."), error: true });
    }
  }

  if (experts.isLoading) return <Loading />;
  return (
    <div className="space-y-6">
      <PageHeader title="Duyệt hồ sơ chuyên gia" description="Kiểm tra thông tin và tài liệu minh chứng trước khi ra quyết định." />
      {actionMessage && <p role="status" className={`rounded-xl p-3 text-sm ${actionMessage.error ? "bg-rose-50 text-rose-700" : "bg-emerald-50 text-emerald-700"}`}>{actionMessage.text}</p>}
      <select className="rounded-xl border p-3" value={status} onChange={(e) => { setStatus(e.target.value); setSelectedId(undefined); }}>
        <option value="PENDING">Chờ duyệt</option>
        <option value="APPROVED">Đã duyệt</option>
        <option value="REJECTED">Đã từ chối</option>
      </select>
      {!experts.data?.content.length && <EmptyState title="Không có hồ sơ trong trạng thái này" />}
      {experts.data?.content.map((item) => (
        <Card className="p-6" key={item.id}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-bold">{item.fullName}</h2>
              <p className="text-muted">{item.email} · {item.headline}</p>
            </div>
            <Button onClick={() => setSelectedId(selectedId === item.id ? undefined : item.id)} variant="outline">
              {selectedId === item.id ? "Thu gọn" : "Xem hồ sơ và minh chứng"}
            </Button>
          </div>
          <div className="mt-4 grid gap-2 text-sm md:grid-cols-2">
            <p><b>Chuyên khoa:</b> {item.specialties || "—"}</p>
            <p><b>Kinh nghiệm:</b> {item.yearsOfExperience ?? 0} năm</p>
            <p><b>Nơi làm việc:</b> {item.workplace || "—"}</p>
            <p><b>Giá:</b> {item.consultationFee?.toLocaleString("vi-VN") ?? "—"} VNĐ</p>
          </div>
          {selectedId === item.id && (
            <div className="mt-5 rounded-xl bg-slate-50 p-4">
              {detail.isLoading ? <Loading /> : (
                <>
                  <p className="whitespace-pre-wrap text-sm"><b>Học vấn:</b> {detail.data?.profile.education || "—"}</p>
                  <p className="mt-2 whitespace-pre-wrap text-sm"><b>Giới thiệu:</b> {detail.data?.profile.bio || "—"}</p>
                  <h3 className="mt-4 font-bold">Tài liệu minh chứng</h3>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {detail.data?.documents.map((doc) => (
                      <Button key={doc.id} onClick={() => { void openDocument(doc.id); }} variant="outline">
                        {doc.title}
                      </Button>
                    ))}
                    {!detail.data?.documents.length && <span className="text-sm text-rose-600">Không có tài liệu.</span>}
                  </div>
                </>
              )}
            </div>
          )}
          {status === "PENDING" && (
            <div className="mt-5 flex gap-3">
              <Button onClick={() => { void review(item.id, "APPROVED", "Hồ sơ hợp lệ"); }}>Duyệt</Button>
              <Button onClick={() => {
                const reason = window.prompt("Lý do từ chối?");
                if (reason?.trim()) void review(item.id, "REJECTED", reason.trim());
              }} variant="outline">Từ chối</Button>
            </div>
          )}
        </Card>
      ))}
    </div>
  );
}

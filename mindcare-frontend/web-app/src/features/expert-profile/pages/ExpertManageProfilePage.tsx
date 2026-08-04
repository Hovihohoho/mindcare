import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button, Card, EmptyState, getApiErrorMessage, Input, Loading, PageHeader, Textarea } from "@/shared";
import { expertProfileApi } from "../api/expertProfile.api";

const statusLabel: Record<string, string> = {
  NONE: "Chưa gửi duyệt",
  PENDING: "Đang chờ duyệt",
  APPROVED: "Đã được duyệt",
  REJECTED: "Cần bổ sung hồ sơ",
};

export function ExpertManageProfilePage() {
  const [message, setMessage] = useState("");
  const profile = useQuery({ queryKey: ["expert-account-profile"], queryFn: expertProfileApi.me });
  const refresh = () => profile.refetch();
  const save = useMutation({
    mutationFn: expertProfileApi.update,
    onSuccess: () => { setMessage("Đã lưu thông tin chuyên môn."); refresh(); },
    onError: () => setMessage("Không thể lưu hồ sơ. Vui lòng kiểm tra dữ liệu."),
  });
  const upload = useMutation({
    mutationFn: ({ file, type, title }: { file: File; type: string; title: string }) =>
      expertProfileApi.uploadDocument(file, type, title),
    onSuccess: () => { setMessage("Đã tải tài liệu minh chứng."); refresh(); },
    onError: () => setMessage("Không thể tải tài liệu. Chỉ nhận ảnh/PDF tối đa 10MB."),
  });
  const submitProfile = useMutation({
    mutationFn: expertProfileApi.submit,
    onSuccess: () => { setMessage("Hồ sơ đã được gửi tới admin."); refresh(); },
    onError: () => setMessage("Hồ sơ chưa đủ thông tin hoặc chưa có tài liệu minh chứng."),
  });

  if (profile.isLoading) return <Loading />;
  const item = profile.data?.profile;
  if (!item) return <EmptyState title="Không thể tải hồ sơ chuyên gia" />;
  const fullName = item.fullName;

  function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    save.mutate({
      fullName,
      headline: String(data.get("headline") || ""),
      specialties: String(data.get("specialties") || ""),
      yearsOfExperience: Number(data.get("yearsOfExperience")),
      consultationFee: Number(data.get("consultationFee")),
      workplace: String(data.get("workplace") || ""),
      education: String(data.get("education") || ""),
      bio: String(data.get("bio") || ""),
    });
  }

  function uploadDocument(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const file = data.get("file");
    if (!(file instanceof File) || !file.size) return setMessage("Vui lòng chọn một ảnh hoặc PDF.");
    upload.mutate({
      file,
      type: String(data.get("documentType") || ""),
      title: String(data.get("title") || ""),
    }, { onSuccess: () => form.reset() });
  }

  async function openDocument(id: string) {
    setMessage("");
    try {
      await expertProfileApi.openDocument(id);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "Không thể mở tài liệu."));
    }
  }

  async function deleteDocument(id: string) {
    if (!window.confirm("Bạn có chắc muốn xóa tài liệu này không?")) return;
    setMessage("");
    try {
      await expertProfileApi.deleteDocument(id);
      await refresh();
      setMessage("Đã xóa tài liệu.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "Không thể xóa tài liệu."));
    }
  }

  const currentStatus = item.expertStatus ?? "NONE";
  return (
    <div className="space-y-7">
      <PageHeader
        title="Hồ sơ chuyên gia"
        description={`Trạng thái: ${statusLabel[currentStatus] ?? currentStatus}`}
      />
      {item.expertReviewReason && (
        <Card className="border-amber-200 bg-amber-50 p-4 text-amber-900">
          <b>Phản hồi từ admin:</b> {item.expertReviewReason}
        </Card>
      )}
      {message && <p className="rounded-xl bg-brand-50 p-3 text-sm text-brand-800">{message}</p>}
      <Card className="p-7">
        <form className="grid gap-5 md:grid-cols-2" onSubmit={saveProfile}>
          <Input defaultValue={item.headline ?? ""} label="Chức danh chuyên môn" name="headline" required />
          <Input defaultValue={item.workplace ?? ""} label="Nơi làm việc" name="workplace" required />
          <Input defaultValue={item.specialties ?? ""} label="Chuyên khoa (phân cách bằng dấu phẩy)" name="specialties" required />
          <Input defaultValue={item.yearsOfExperience ?? 0} label="Số năm kinh nghiệm" min={0} name="yearsOfExperience" type="number" required />
          <Input defaultValue={item.consultationFee ?? 0} label="Giá tư vấn (VNĐ)" min={0} name="consultationFee" type="number" required />
          <Input defaultValue={item.education ?? ""} label="Học vấn" name="education" required />
          <div className="md:col-span-2">
            <Textarea defaultValue={item.bio ?? ""} label="Giới thiệu chuyên môn" name="bio" rows={5} />
          </div>
          <Button className="w-fit" loading={save.isPending}>Lưu hồ sơ</Button>
        </form>
      </Card>
      <Card className="p-7">
        <h2 className="text-xl font-bold">Bằng cấp và chứng chỉ</h2>
        <p className="mt-1 text-sm text-muted">Tải ảnh hoặc PDF rõ nét, dung lượng tối đa 10MB.</p>
        <form className="mt-5 grid gap-4 md:grid-cols-3" onSubmit={uploadDocument}>
          <Input label="Loại tài liệu" name="documentType" placeholder="BẰNG CẤP / CHỨNG CHỈ" required />
          <Input label="Tên tài liệu" name="title" required />
          <Input accept=".jpg,.jpeg,.png,.webp,.pdf" label="Tệp minh chứng" name="file" type="file" required />
          <Button className="w-fit" loading={upload.isPending}>Tải tài liệu</Button>
        </form>
        <div className="mt-5 divide-y">
          {profile.data?.documents.map((doc) => (
            <div className="flex items-center justify-between gap-4 py-3" key={doc.id}>
              <button className="text-left font-semibold text-brand-700 hover:underline" onClick={() => { void openDocument(doc.id); }} type="button">
                {doc.title} <span className="text-xs font-normal text-muted">({doc.documentType})</span>
              </button>
              <Button onClick={() => { void deleteDocument(doc.id); }} variant="outline">Xóa</Button>
            </div>
          ))}
          {!profile.data?.documents.length && <p className="py-5 text-sm text-muted">Chưa có tài liệu minh chứng.</p>}
        </div>
      </Card>
      <Button
        disabled={currentStatus === "PENDING" || currentStatus === "APPROVED"}
        loading={submitProfile.isPending}
        onClick={() => submitProfile.mutate()}
        size="lg"
      >
        Gửi hồ sơ để admin duyệt
      </Button>
    </div>
  );
}

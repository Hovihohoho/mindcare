import { useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Download, Save, Trash2, Upload } from "lucide-react";
import { authApi, useCurrentUser } from "@/features/auth";
import { Avatar, Button, Card, Input, Loading, PageHeader, Textarea } from "@/shared";
import { userStorage } from "@/shared/lib/storage";
import { tokenStorage } from "@/shared/lib/storage";
import { privacyApi } from "../api/privacy.api";

export function ProfilePage() {
  const user = useCurrentUser();
  const client = useQueryClient();
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const save = useMutation({
    mutationFn: authApi.updateMe,
    onSuccess: (item) => {
      userStorage.set(item);
      client.setQueryData(["current-user"], item);
    },
  });
  const avatar = useMutation({
    mutationFn: authApi.uploadAvatar,
    onSuccess: (item) => {
      userStorage.set(item);
      client.setQueryData(["current-user"], item);
    },
  });
  const exportData = useMutation({
    mutationFn: async () => {
      const [account, wellbeing, ai] = await Promise.all([
        authApi.exportMyData(),
        privacyApi.exportEmotionData(),
        privacyApi.exportAiData(),
      ]);
      const blob = new Blob([JSON.stringify({ exportedAt: new Date().toISOString(), account, wellbeing, ai }, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `mindcare-data-${new Date().toISOString().slice(0, 10)}.json`;
      link.click();
      URL.revokeObjectURL(url);
    },
  });
  const deleteAccount = useMutation({
    mutationFn: async () => {
      const password = window.prompt("Nhập mật khẩu hiện tại để xác nhận xóa vĩnh viễn tài khoản");
      if (!password) throw new Error("CANCELLED");
      await authApi.verifyPassword(password);
      await privacyApi.deleteEmotionData();
      await privacyApi.deleteAiData();
      await authApi.permanentlyDelete(password);
    },
    onSuccess: () => {
      tokenStorage.clear();
      window.location.assign("/login");
    },
  });

  if (user.isLoading) return <Loading />;
  if (!user.data) return <p className="text-rose-600">Không thể tải hồ sơ.</p>;

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    save.mutate({
      fullName: String(data.get("fullName")),
      phone: String(data.get("phone") || "") || null,
      birthDate: String(data.get("birthDate") || "") || null,
      gender: String(data.get("gender") || "") || null,
      address: String(data.get("address") || "") || null,
      bio: String(data.get("bio") || "") || null,
    });
  }

  return (
    <div className="space-y-7">
      <PageHeader title="Hồ sơ cá nhân" description="Thông tin giúp MindCare hỗ trợ bạn phù hợp hơn." />
      <Card className="flex flex-wrap items-center gap-5 p-7">
        <Avatar
          alt={user.data.fullName}
          className="size-24 text-2xl"
          fallback={user.data.fullName}
          src={user.data.avatarUrl ?? undefined}
        />
        <div className="flex-1">
          <h2 className="text-xl font-bold">{user.data.fullName}</h2>
          <p className="text-muted">{user.data.email}</p>
        </div>
        <div>
          <input
            ref={avatarInputRef}
            className="hidden"
            accept="image/png,image/jpeg,image/webp"
            type="file"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) avatar.mutate(file);
              event.target.value = "";
            }}
          />
          <Button
            leftIcon={<Upload className="size-4" />}
            loading={avatar.isPending}
            onClick={() => avatarInputRef.current?.click()}
            type="button"
            variant="outline"
          >
            Tải ảnh đại diện
          </Button>
          {avatar.isSuccess && <p className="mt-2 text-sm text-emerald-700">Đã cập nhật ảnh đại diện.</p>}
          {avatar.isError && <p className="mt-2 max-w-64 text-sm text-rose-600">Không thể tải ảnh. Chỉ nhận JPEG, PNG hoặc WebP tối đa 5MB.</p>}
        </div>
      </Card>
      <Card className="p-7">
        <form className="grid gap-5 md:grid-cols-2" onSubmit={submit}>
          <Input defaultValue={user.data.fullName} label="Họ và tên" name="fullName" required />
          <Input defaultValue={user.data.phone ?? ""} label="Số điện thoại" name="phone" />
          <Input defaultValue={user.data.birthDate ?? ""} label="Ngày sinh" name="birthDate" type="date" />
          <label className="space-y-2 text-sm font-semibold">
            Giới tính
            <select className="h-12 w-full rounded-xl border border-line px-4" defaultValue={user.data.gender ?? ""} name="gender">
              <option value="">Chưa chọn</option>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </select>
          </label>
          <div className="md:col-span-2"><Textarea defaultValue={user.data.address ?? ""} label="Địa chỉ" name="address" /></div>
          <div className="md:col-span-2"><Textarea defaultValue={user.data.bio ?? ""} label="Giới thiệu" name="bio" rows={5} /></div>
          {save.isSuccess && <p className="text-emerald-700 md:col-span-2">Đã lưu hồ sơ.</p>}
          {save.isError && <p className="text-rose-600 md:col-span-2">Không thể lưu hồ sơ.</p>}
          <div className="md:col-span-2"><Button leftIcon={<Save className="size-4" />} loading={save.isPending}>Lưu thay đổi</Button></div>
        </form>
      </Card>
      <Card className="p-7">
        <h2 className="text-lg font-bold">Quyền dữ liệu của bạn</h2>
        <p className="mt-2 text-sm text-muted">Tải bản sao dữ liệu hồ sơ, nhật ký, đánh giá, Health Connect, kế hoạch tự chăm sóc và hội thoại AI.</p>
        <Button className="mt-5" leftIcon={<Download className="size-4" />} loading={exportData.isPending} onClick={() => exportData.mutate()} type="button" variant="outline">Tải dữ liệu của tôi</Button>
        {exportData.isError && <p className="mt-2 text-sm text-rose-600">Không thể tạo bản xuất dữ liệu. Vui lòng thử lại.</p>}
      </Card>
      <Card className="border-rose-200 p-7">
        <h2 className="text-lg font-bold text-rose-800">Xóa tài khoản vĩnh viễn</h2>
        <p className="mt-2 text-sm text-muted">Thao tác này xóa hồ sơ, dữ liệu chăm sóc tinh thần và hội thoại AI. Dữ liệu không thể khôi phục.</p>
        <Button className="mt-5" leftIcon={<Trash2 className="size-4" />} loading={deleteAccount.isPending} onClick={() => { if (window.confirm("Bạn chắc chắn muốn xóa vĩnh viễn toàn bộ tài khoản MindCare?")) deleteAccount.mutate(); }} type="button" variant="outline">Xóa vĩnh viễn tài khoản</Button>
        {deleteAccount.isError && <p className="mt-2 text-sm text-rose-600">Không thể hoàn tất xóa tài khoản. Nếu một phần dữ liệu đã được xóa, bạn có thể thử lại an toàn.</p>}
      </Card>
    </div>
  );
}

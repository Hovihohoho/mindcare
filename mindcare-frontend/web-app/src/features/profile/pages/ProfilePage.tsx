import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save, UserRound } from "lucide-react";
import { authApi } from "@/features/auth";
import { Avatar, Button, Card, Input, Loading, PageHeader, Textarea } from "@/shared";
import { userStorage } from "@/shared/lib/storage";

export function ProfilePage() {
  const client = useQueryClient();
  const profile = useQuery({ queryKey: ["auth-me"], queryFn: authApi.me });
  const update = useMutation({
    mutationFn: authApi.updateMe,
    onSuccess: (user) => {
      userStorage.set(user);
      client.setQueryData(["auth-me"], user);
    },
  });
  if (profile.isLoading) return <Loading />;
  if (!profile.data) return <p className="text-rose-600">Không thể tải hồ sơ.</p>;
  const user = profile.data;

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = Object.fromEntries(new FormData(event.currentTarget));
    update.mutate({
      fullName: String(value.fullName),
      phone: String(value.phone) || null,
      birthDate: String(value.birthDate) || null,
      gender: String(value.gender) || null,
      address: String(value.address) || null,
      bio: String(value.bio) || null,
    });
  }

  return (
    <div className="space-y-7">
      <PageHeader title="Hồ sơ cá nhân" description="Cập nhật thông tin để MindCare hỗ trợ bạn tốt hơn." />
      <Card className="flex items-center gap-5 p-7"><Avatar className="size-24 text-2xl" fallback={user.fullName} /><div><h2 className="text-2xl font-bold">{user.fullName}</h2><p className="mt-1 text-muted">{user.email}</p></div></Card>
      <Card className="p-7">
        <h2 className="flex items-center gap-2 text-xl font-semibold"><UserRound className="text-brand-700" />Thông tin của bạn</h2>
        <form className="mt-6 grid gap-5 md:grid-cols-2" onSubmit={submit}>
          <Input defaultValue={user.fullName} label="Họ và tên" name="fullName" required />
          <Input defaultValue={user.phone ?? ""} label="Số điện thoại" name="phone" />
          <Input defaultValue={user.birthDate ?? ""} label="Ngày sinh" name="birthDate" type="date" />
          <label className="space-y-2 text-sm font-semibold text-slate-700">Giới tính<select className="h-12 w-full rounded-xl border border-line bg-slate-50 px-4 font-normal" defaultValue={user.gender ?? ""} name="gender"><option value="">Chưa chọn</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></label>
          <div className="md:col-span-2"><Textarea defaultValue={user.address ?? ""} label="Địa chỉ" name="address" /></div>
          <div className="md:col-span-2"><Textarea defaultValue={user.bio ?? ""} label="Giới thiệu bản thân" name="bio" rows={5} /></div>
          {update.isError && <p className="text-sm text-rose-600 md:col-span-2">Không thể lưu hồ sơ.</p>}
          {update.isSuccess && <p className="text-sm text-emerald-700 md:col-span-2">Đã cập nhật hồ sơ.</p>}
          <div className="md:col-span-2"><Button leftIcon={<Save className="size-4" />} loading={update.isPending} type="submit">Lưu thay đổi</Button></div>
        </form>
      </Card>
    </div>
  );
}

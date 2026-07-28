import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BriefcaseBusiness, Save } from "lucide-react";
import { authApi } from "@/features/auth";
import { Avatar, Button, Card, Input, Loading, Textarea } from "@/shared";
import { userStorage } from "@/shared/lib/storage";

export function ExpertManageProfilePage() {
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
  if (!profile.data) return <p className="text-rose-600">Không thể tải hồ sơ chuyên gia.</p>;
  const user = profile.data;

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = Object.fromEntries(new FormData(event.currentTarget));
    update.mutate({
      fullName: String(value.fullName),
      phone: String(value.phone) || null,
      address: String(value.address) || null,
      bio: String(value.bio) || null,
      headline: String(value.headline) || null,
      specialties: String(value.specialties) || null,
      yearsOfExperience: value.yearsOfExperience ? Number(value.yearsOfExperience) : null,
      consultationFee: value.consultationFee ? Number(value.consultationFee) : null,
      workplace: String(value.workplace) || null,
      education: String(value.education) || null,
    });
  }

  return (
    <div className="mx-auto max-w-[1000px] space-y-6">
      <header><h1 className="text-3xl font-bold">Hồ sơ chuyên gia</h1><p className="mt-2 text-slate-600">Thông tin chuyên môn hiển thị trong hệ thống MindCare.</p></header>
      <Card className="flex items-center gap-5 p-7"><Avatar className="size-24 text-2xl" fallback={user.fullName} /><div><h2 className="text-2xl font-bold">{user.fullName}</h2><p className="mt-1 text-muted">{user.email}</p></div></Card>
      <Card className="p-7">
        <h2 className="flex items-center gap-2 text-xl font-semibold"><BriefcaseBusiness className="text-brand-700" />Thông tin chuyên môn</h2>
        <form className="mt-6 grid gap-5 md:grid-cols-2" onSubmit={submit}>
          <Input defaultValue={user.fullName} label="Họ và tên" name="fullName" required />
          <Input defaultValue={user.phone ?? ""} label="Số điện thoại" name="phone" />
          <Input defaultValue={user.headline ?? ""} label="Chức danh / tiêu đề" name="headline" placeholder="Chuyên gia tâm lý lâm sàng" />
          <Input defaultValue={user.workplace ?? ""} label="Nơi công tác" name="workplace" />
          <Input defaultValue={user.yearsOfExperience ?? ""} label="Số năm kinh nghiệm" min={0} name="yearsOfExperience" type="number" />
          <Input defaultValue={user.consultationFee ?? ""} label="Phí tư vấn mỗi phiên (VND)" min={0} name="consultationFee" type="number" />
          <div className="md:col-span-2"><Input defaultValue={user.specialties ?? ""} hint="Phân cách bằng dấu phẩy" label="Chuyên môn" name="specialties" /></div>
          <div className="md:col-span-2"><Textarea defaultValue={user.education ?? ""} label="Học vấn và chứng chỉ" name="education" rows={4} /></div>
          <div className="md:col-span-2"><Textarea defaultValue={user.address ?? ""} label="Địa chỉ làm việc" name="address" /></div>
          <div className="md:col-span-2"><Textarea defaultValue={user.bio ?? ""} label="Giới thiệu chuyên gia" name="bio" rows={6} /></div>
          {update.isError && <p className="text-sm text-rose-600 md:col-span-2">Không thể lưu hồ sơ chuyên gia.</p>}
          {update.isSuccess && <p className="text-sm text-emerald-700 md:col-span-2">Đã cập nhật hồ sơ chuyên gia.</p>}
          <div className="md:col-span-2"><Button leftIcon={<Save className="size-4" />} loading={update.isPending} type="submit">Lưu hồ sơ</Button></div>
        </form>
      </Card>
    </div>
  );
}

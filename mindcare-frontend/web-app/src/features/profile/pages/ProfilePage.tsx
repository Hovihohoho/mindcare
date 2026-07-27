import { Pencil, Save, UserRound } from "lucide-react";
import { Avatar, Badge, Button, Card, Input, Textarea } from "@/shared";
import { ProfileStats } from "../components/ProfileStats";

export function ProfilePage() {
  return (
    <div className="space-y-8">
      <Card className="p-7">
        <div className="flex flex-col gap-7 md:flex-row md:items-center">
          <div className="relative shrink-0"><Avatar className="size-32 border-4 border-sky-200 text-2xl" fallback="NA" /><button className="absolute bottom-0 right-0 grid size-10 place-items-center rounded-full bg-brand-700 text-white shadow"><Pencil className="size-4" /></button></div>
          <div className="flex-1"><h1 className="text-xl font-medium">Nguyễn Văn A</h1><p className="mt-3 text-slate-500">nguyenvana@email.com</p><div className="mt-4"><Textarea className="min-h-28 bg-slate-50 text-base" defaultValue="Hiện tại đang là sinh viên năm cuối ngành công nghệ thông tin tại trường Đại học Công nghiệp Hồ Chí Minh." /></div></div>
        </div>
      </Card>
      <ProfileStats />
      <Card className="p-8">
        <h2 className="flex items-center gap-3 text-xl font-medium"><UserRound className="text-slate-700" />Thông tin cá nhân</h2>
        <div className="mt-8 grid gap-6 sm:grid-cols-2"><Input label="Họ và Tên" defaultValue="Nguyễn Minh Tâm" /><Input label="Ngày sinh" type="date" defaultValue="1998-05-15" /><label className="space-y-2 text-sm font-semibold text-slate-700">Giới tính<select className="mt-2 h-12 w-full rounded-xl border border-line bg-slate-50 px-4 font-normal"><option>Nữ</option><option>Nam</option></select></label><Input label="Số điện thoại" defaultValue="090 123 4567" /><div className="sm:col-span-2"><Textarea label="Địa chỉ hiện tại" defaultValue="123 Đường Điện Biên Phủ, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh" /></div><div className="sm:col-span-2"><label className="text-sm font-semibold text-slate-700">Sở thích</label><div className="mt-2 rounded-xl border border-line bg-slate-50 p-4"><div className="flex gap-2"><Badge>Thiền định ×</Badge><Badge>Đọc sách ×</Badge></div><p className="mt-3 text-slate-500">Thêm sở thích...</p></div></div></div>
      </Card>
      <div className="flex justify-end"><Button size="lg" leftIcon={<Save className="size-5" />}>Lưu thay đổi</Button></div>
    </div>
  );
}

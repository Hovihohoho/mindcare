import { Bell, Globe2, LockKeyhole } from "lucide-react";
import { Button, Card, PageHeader } from "@/shared";
import { SettingRow } from "../components/SettingRow";

function Toggle({ defaultChecked = true }: { defaultChecked?: boolean }) {
  return <label className="relative inline-flex cursor-pointer"><input className="peer sr-only" type="checkbox" defaultChecked={defaultChecked} /><span className="h-6 w-11 rounded-full bg-slate-300 transition peer-checked:bg-brand-600 after:absolute after:left-1 after:top-1 after:size-4 after:rounded-full after:bg-white after:transition peer-checked:after:translate-x-5" /></label>;
}

export function SettingsPage() {
  return (
    <div className="space-y-7">
      <PageHeader title="Cài đặt hệ thống" description="Quản lý tài khoản và tùy chỉnh trải nghiệm MindCare của bạn." />
      <Card className="p-6"><h2 className="flex items-center gap-3 text-lg font-extrabold"><span className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-600"><Bell className="size-5" /></span>Thông báo</h2><div className="mt-3"><SettingRow title="Thông báo qua Email" description="Nhận bản tin hàng tuần và cập nhật liệu trình." control={<Toggle />} /><SettingRow title="Thông báo trong ứng dụng" description="Thông báo về trạng thái tâm trạng và AI gợi ý." control={<Toggle />} /></div></Card>
      <Card className="p-6"><h2 className="flex items-center gap-3 text-lg font-extrabold"><span className="grid size-10 place-items-center rounded-xl bg-violet-50 text-violet-600"><LockKeyhole className="size-5" /></span>Bảo mật</h2><SettingRow title="Đổi mật khẩu" description="Cập nhật mật khẩu mới cho tài khoản." control={<Button variant="outline">Cập nhật</Button>} /></Card>
      <Card className="p-6"><h2 className="flex items-center gap-3 text-lg font-extrabold"><span className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-600"><Globe2 className="size-5" /></span>Ngôn ngữ</h2><div className="mt-4 space-y-3"><label className="flex items-center gap-3 rounded-xl border border-brand-600 bg-brand-50 p-4"><input type="radio" name="language" defaultChecked /><span><b>Tiếng Việt</b><small className="block text-muted">Ngôn ngữ mặc định của ứng dụng</small></span></label><label className="flex items-center gap-3 rounded-xl border border-line p-4"><input type="radio" name="language" /><span><b>English (US)</b><small className="block text-muted">Secondary language support</small></span></label></div></Card>
      <div className="flex justify-end gap-3"><Button variant="ghost">Hủy</Button><Button>Lưu thay đổi</Button></div>
    </div>
  );
}

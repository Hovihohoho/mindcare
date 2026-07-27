import { BadgeCheck, Camera, Mail, MapPin, Phone } from "lucide-react";
import { Avatar, Badge, Card } from "@/shared";

export function ExpertProfileSummary() {
  return (
    <div className="space-y-6">
      <Card className="p-7">
        <div className="flex flex-col items-center text-center">
          <div className="relative"><Avatar className="size-36 border-4 border-blue-500 text-3xl ring-4 ring-blue-100" fallback="MA" /><button className="absolute bottom-0 right-0 grid size-10 place-items-center rounded-full bg-blue-600 text-white shadow"><Camera className="size-5" /></button></div>
          <h2 className="mt-8 flex items-center gap-2 text-xl font-medium">BS. Minh Anh <BadgeCheck className="size-5 fill-emerald-700 text-white" /></h2>
          <Badge className="mt-3" tone="success">Đã xác minh</Badge>
        </div>
        <div className="mt-8 space-y-6 border-t border-line pt-7">
          <div className="flex gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-600"><Mail className="size-5" /></span><div><p className="text-sm text-slate-600">Email cá nhân</p><b>minhanh.psy@tam</b></div></div>
          <div className="flex gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-600"><Phone className="size-5" /></span><div><p className="text-sm text-slate-600">Số điện thoại</p><b>0987.654.321</b></div></div>
          <div className="flex gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-600"><MapPin className="size-5" /></span><div><p className="text-sm text-slate-600">Địa chỉ làm việc</p><b>Cầu Giấy, Hà Nội</b></div></div>
        </div>
      </Card>
      <Card className="p-7"><div className="flex items-center justify-between"><h3 className="text-lg font-medium">Trạng thái hoạt động</h3><span className="size-3 rounded-full bg-emerald-700" /></div><div className="mt-6 grid grid-cols-2 divide-x divide-slate-300 rounded-xl bg-blue-50 p-6 text-center"><div><b className="text-lg text-blue-700">1.2k</b><p className="text-slate-600">Bệnh nhân</p></div><div><b className="text-lg text-emerald-700">4.9</b><p className="text-slate-600">Đánh giá</p></div></div></Card>
    </div>
  );
}

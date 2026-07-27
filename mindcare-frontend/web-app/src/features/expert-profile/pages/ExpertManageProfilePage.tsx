import { Award, BriefcaseBusiness, Building2, Download, Eye, FileCheck2, Pencil, Save, Upload } from "lucide-react";
import { Button, Card } from "@/shared";
import { ExpertProfileSummary } from "../components/ExpertProfileSummary";

const certificates = [
  { name: "Bang-thac-si.pdf", icon: FileCheck2, tone: "text-red-500 bg-red-50" },
  { name: "Chung-chi-CBT-Advanced.jpg", icon: Eye, tone: "text-blue-500 bg-blue-50" },
  { name: "Giay-phep-hanh-nghe.pdf", icon: FileCheck2, tone: "text-red-500 bg-red-50" },
];

export function ExpertManageProfilePage() {
  return (
    <div className="mx-auto max-w-[1200px] space-y-7">
      <div className="grid items-start gap-6 xl:grid-cols-[350px_1fr]">
        <ExpertProfileSummary />
        <div className="space-y-6">
          <Card className="p-8">
            <h2 className="flex items-center gap-3 text-xl font-medium"><span className="grid size-10 place-items-center rounded-xl bg-blue-50 text-blue-600"><BriefcaseBusiness className="size-5" /></span>Thông tin chuyên môn</h2>
            <div className="mt-8 grid gap-x-14 gap-y-7 md:grid-cols-2">
              <div><p className="text-slate-600">Chuyên ngành đào tạo</p><b className="mt-2 flex items-center gap-2"><Award className="size-5 text-blue-600" />Tâm lý học lâm sàng</b></div>
              <div><p className="text-slate-600">Nơi công tác hiện tại</p><b className="mt-2 flex items-center gap-2"><Building2 className="size-5 text-blue-600" />Bệnh viện Tâm thần Trung ương</b></div>
              <div><p className="text-slate-600">Học hàm/Học vị</p><b className="mt-2 flex items-center gap-2"><Award className="size-5 text-blue-600" />Thạc sĩ Tâm lý học</b></div>
              <div className="rounded-xl bg-blue-50 p-5 text-blue-700"><p className="font-medium">Giá tư vấn / 60 phút</p><b className="text-xl">150.000đ</b></div>
              <div><p className="text-slate-600">Số năm kinh nghiệm</p><b className="mt-2 block">10 năm kinh nghiệm</b></div>
            </div>
          </Card>
          <Card className="p-8"><h2 className="flex items-center gap-3 text-xl font-medium"><span className="grid size-10 place-items-center rounded-xl bg-blue-50 text-blue-600"><FileCheck2 className="size-5" /></span>Giới thiệu bản thân</h2><p className="mt-8 leading-8 text-slate-600">Với hơn 10 năm kinh nghiệm trong lĩnh vực Tâm lý học lâm sàng, tôi luôn tâm niệm rằng sức khỏe tinh thần là nền tảng của một cuộc sống hạnh phúc. Triết lý làm việc của tôi dựa trên sự lắng nghe sâu sắc, tôn trọng thế giới quan của bệnh nhân và đồng hành cùng họ trong việc tìm lại sự cân bằng nội tâm thông qua các liệu pháp nhận thức hành vi (CBT) và liệu pháp chấp nhận cam kết (ACT).</p><p className="mt-5 leading-8 text-slate-600">Tôi đã hỗ trợ hàng ngàn trường hợp gặp khó khăn về rối loạn lo âu, trầm cảm và các vấn đề về mối quan hệ, giúp họ xây dựng khả năng phục hồi và phát triển bản thân bền vững.</p></Card>
        </div>
      </div>
      <Card className="p-8"><div className="flex flex-wrap items-center justify-between gap-4"><h2 className="flex items-center gap-3 text-lg font-medium"><Award className="text-blue-600" />Danh sách chứng chỉ & Bằng cấp</h2><Button variant="secondary">＋ Thêm chứng chỉ</Button></div><div className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-4">{certificates.map(({ name, icon: Icon, tone }) => <div className="flex min-h-28 items-center gap-4 rounded-xl border border-line p-5" key={name}><span className={`grid size-12 shrink-0 place-items-center rounded-xl ${tone}`}><Icon /></span><b className="min-w-0 flex-1 break-words">{name}</b><Download className="size-5 text-slate-600" /></div>)}<button className="flex min-h-28 flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 text-slate-600"><Upload className="size-7" /><span className="mt-2">Tải tài liệu mới</span></button></div></Card>
      <div className="flex justify-end gap-4"><Button size="lg" variant="outline" leftIcon={<Pencil className="size-5" />}>Chỉnh sửa thông tin</Button><Button size="lg" leftIcon={<Save className="size-5" />}>Lưu thay đổi</Button></div>
    </div>
  );
}

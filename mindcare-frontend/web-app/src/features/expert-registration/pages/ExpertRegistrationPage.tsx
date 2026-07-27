import { Plus, Send } from "lucide-react";
import { Badge, Button, Input, PageHeader, Textarea } from "@/shared";
import { RegistrationSection } from "../components/RegistrationSection";
import { UploadField } from "../components/UploadField";

export function ExpertRegistrationPage() {
  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Trở thành chuyên gia MindCare" title="Đăng ký hồ sơ chuyên gia" description="Cung cấp thông tin chính xác để đội ngũ MindCare xác thực năng lực chuyên môn của bạn." />
      <RegistrationSection step="1" title="Thông tin cá nhân" description="Thông tin cơ bản dùng cho quá trình xác minh.">
        <div className="grid gap-4 md:grid-cols-2"><Input label="Họ và tên *" placeholder="Nguyễn Thị Minh Anh" /><Input label="Ngày sinh *" type="date" /><Input label="Giới tính *" placeholder="Nữ" /><Input label="Email *" type="email" placeholder="minhanh.nguyen@mindcare.vn" /><Input label="Số điện thoại *" /><Input label="Địa chỉ" placeholder="123 Nguyễn Huệ, Quận 1, TP.HCM" /></div>
      </RegistrationSection>
      <RegistrationSection step="2" title="Thông tin chuyên môn" description="Giúp người dùng hiểu lĩnh vực và kinh nghiệm của bạn.">
        <div className="grid gap-4 md:grid-cols-2"><Input label="Chức danh *" placeholder="Chọn chức danh" /><Input label="Chuyên ngành đào tạo *" placeholder="VD: Tâm lý học lâm sàng" /><Input label="Số năm kinh nghiệm *" type="number" /><Input label="Đơn vị công tác hiện tại" placeholder="Tên bệnh viện, phòng khám..." /></div>
        <div className="mt-5"><p className="text-sm font-bold">Lĩnh vực tư vấn chuyên sâu *</p><div className="mt-3 flex flex-wrap gap-2">{["Trầm cảm", "Rối loạn lo âu", "Tâm lý học đường", "Mối quan hệ", "Căng thẳng"].map((item) => <Badge className="px-4 py-2" key={item}>{item}</Badge>)}<Button size="sm" variant="outline" leftIcon={<Plus className="size-4" />}>Thêm lĩnh vực</Button></div></div>
        <div className="mt-5 grid gap-4"><Textarea label="Tóm tắt tiểu sử & Giới thiệu *" placeholder="Viết một đoạn ngắn giới thiệu về bản thân..." /><Textarea label="Triết lý hành nghề" placeholder="Phương pháp tiếp cận chính của bạn..." /><Textarea label="Điểm mạnh chuyên môn" /></div>
      </RegistrationSection>
      <RegistrationSection step="3" title="Bằng cấp & Chứng chỉ" description="Thêm các văn bằng có liên quan đến chuyên môn.">
        <div className="rounded-xl border border-line p-4"><div className="grid gap-4 md:grid-cols-2"><Input label="Tên bằng cấp *" defaultValue="Cử nhân Tâm lý học" /><Input label="Trường đào tạo *" defaultValue="Đại học KHXH&NV" /><Input label="Năm tốt nghiệp" type="number" /><Input label="Loại bằng" defaultValue="Giỏi" /></div><UploadField label="Bản sao bằng cấp" /></div>
        <Button className="mt-4" variant="outline" leftIcon={<Plus className="size-4" />}>Thêm bằng cấp</Button>
      </RegistrationSection>
      <RegistrationSection step="4" title="Kinh nghiệm làm việc" description="Mô tả các vị trí công tác liên quan.">
        <div className="grid gap-4 md:grid-cols-2"><Input label="Đơn vị công tác *" placeholder="Tên bệnh viện, trung tâm..." /><Input label="Chức vụ" placeholder="Chuyên viên tư vấn" /><Input label="Thời gian bắt đầu" type="date" /><Input label="Thời gian kết thúc" type="date" /><Textarea className="md:col-span-2" label="Mô tả công việc" placeholder="Mô tả ngắn gọn công việc và trách nhiệm chính..." /></div>
      </RegistrationSection>
      <RegistrationSection step="5" title="Xác minh danh tính" description="Tài liệu chỉ được dùng cho mục đích kiểm duyệt hồ sơ.">
        <div className="grid gap-5 md:grid-cols-2"><UploadField label="Mặt trước CCCD/CMND *" /><UploadField label="Mặt sau CCCD/CMND *" /><UploadField label="Ảnh chân dung *" /><UploadField label="Ảnh chứng chỉ gốc *" multiple /></div>
      </RegistrationSection>
      <div className="surface p-6"><label className="flex gap-3 text-sm"><input className="mt-1" type="checkbox" /><span>Tôi cam kết các thông tin cung cấp là chính xác và đồng ý với Điều khoản sử dụng, Chính sách bảo mật của MindCare.</span></label><div className="mt-5 flex justify-end"><Button size="lg" leftIcon={<Send className="size-4" />}>Gửi hồ sơ xét duyệt</Button></div></div>
    </div>
  );
}

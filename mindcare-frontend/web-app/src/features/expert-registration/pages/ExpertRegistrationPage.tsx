import { Card, PageHeader } from "@/shared";

export function ExpertRegistrationPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Đăng ký hồ sơ chuyên gia" description="Gửi hồ sơ xét duyệt chuyên gia." />
      <Card className="p-10 text-center text-muted">
        Backend chưa có schema, API upload tài liệu hoặc quy trình xét duyệt hồ sơ chuyên gia. Form giả lập đã được loại bỏ.
      </Card>
    </div>
  );
}

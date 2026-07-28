import { Card, PageHeader } from "@/shared";

export function SettingsPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Cài đặt hệ thống" description="Tùy chỉnh tài khoản MindCare." />
      <Card className="p-10 text-center text-muted">
        Auth Service chưa cung cấp API lưu tùy chọn thông báo, ngôn ngữ hoặc đổi mật khẩu. Các control giả lập đã được loại bỏ.
      </Card>
    </div>
  );
}

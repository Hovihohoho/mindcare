import { Bookmark } from "lucide-react";
import { Card, PageHeader } from "@/shared";

export function BookmarkPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Nội dung đã lưu" description="Danh sách bookmark của tài khoản." />
      <Card className="p-10 text-center">
        <Bookmark className="mx-auto size-10 text-muted" />
        <p className="mt-4 text-muted">Backend chưa cung cấp API bookmark. Không có dữ liệu giả lập được hiển thị.</p>
      </Card>
    </div>
  );
}

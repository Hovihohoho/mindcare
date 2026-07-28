import { Bookmark } from "lucide-react";
import { EmptyState, PageHeader } from "@/shared";

export function BookmarkPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Nội dung đã lưu" description="Quản lý bài đánh giá và chuyên gia bạn quan tâm." />
      <EmptyState
        title="Tính năng lưu nội dung chưa khả dụng"
        description="Backend hiện chưa có API bookmark. Dữ liệu minh họa cũ đã được loại bỏ để tránh hiển thị thông tin không có thật."
        action={<Bookmark className="mx-auto text-brand-700" />}
      />
    </div>
  );
}

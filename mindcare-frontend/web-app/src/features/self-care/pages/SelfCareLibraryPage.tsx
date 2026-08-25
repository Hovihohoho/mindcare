import { useQuery } from "@tanstack/react-query";
import { BookHeart } from "lucide-react";
import { Card, Loading, PageHeader } from "@/shared";
import { selfCareApi } from "../api/selfCare.api";
import { SelfCareCard } from "../components/SelfCareCard";

export function SelfCareLibraryPage() {
  const contents = useQuery({ queryKey: ["self-care-content"], queryFn: selfCareApi.list });
  if (contents.isLoading) return <Loading />;
  return <div className="space-y-8">
    <PageHeader title="Góc tự chăm sóc" description="Nội dung thực hành và kiến thức từ các nguồn đã được kiểm duyệt." />
    {contents.isError && <Card className="p-8 text-center text-rose-700">Không thể tải nội dung lúc này. Vui lòng thử lại sau.</Card>}
    {!contents.isError && contents.data?.length === 0 && <Card className="p-10 text-center"><BookHeart className="mx-auto size-10 text-muted" /><p className="mt-4 text-muted">Chưa có nội dung nào được xuất bản.</p></Card>}
    <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{contents.data?.map((item) => <SelfCareCard item={item} key={item.id} />)}</div>
  </div>;
}

import { BrainCircuit, Search } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, EmptyState, Input, Loading } from "@/shared";
import { ExpertCard } from "../components/ExpertCard";
import { useExperts } from "../hooks/useExperts";
import { ChatHistoryShortcut } from "@/features/expert-chat/components/ChatHistoryShortcut";

export function ExpertDirectoryPage() {
  const [keyword, setKeyword] = useState("");
  const [specialty, setSpecialty] = useState("");
  const catalog = useExperts("", "");
  const experts = useExperts(keyword, specialty);
  const specialties = Array.from(
    new Set(catalog.data?.items.flatMap((expert) => expert.specialties) ?? []),
  ).sort();
  return (
    <div>
      <header className="mx-auto max-w-2xl py-5 text-center md:py-10">
        <h1 className="text-lg font-medium text-slate-800">Kết nối với Chuyên gia</h1>
        <p className="mt-4 leading-6 text-slate-500">Tìm kiếm sự đồng hành từ các chuyên gia tâm lý hàng đầu để bắt đầu hành trình chăm sóc sức khỏe tinh thần của bạn một cách an toàn và hiệu quả.</p>
      </header>
      <ChatHistoryShortcut />
      <section className="mt-14 grid gap-4 rounded-xl border border-line bg-white p-5 lg:grid-cols-[1fr_175px_150px_145px] lg:items-end">
        <Input label="Tìm theo tên hoặc chuyên môn" leading={<Search className="size-4" />} placeholder="Tìm kiếm chuyên gia..." value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        <label className="space-y-2 text-sm font-medium">Chuyên môn<select className="mt-2 h-11 w-full rounded-xl border border-line bg-white px-3" value={specialty} onChange={(event) => setSpecialty(event.target.value)}><option value="">Tất cả</option>{specialties.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
        <label className="space-y-2 text-sm font-medium">Kinh nghiệm<select className="mt-2 h-11 w-full rounded-xl border border-line bg-white px-3"><option>Mọi cấp độ</option></select></label>
        <Button className="w-full">Lọc kết quả</Button>
      </section>
      {experts.isLoading ? <Loading /> : experts.isError ? (
        <EmptyState title="Không thể tải danh sách chuyên gia" description="Vui lòng thử lại sau." />
      ) : experts.data?.items.length ? (
        <div className="mx-auto mt-16 grid max-w-[1080px] gap-6 md:grid-cols-2 xl:grid-cols-3">{experts.data.items.map((expert) => <ExpertCard key={expert.expertUserId} expert={expert} />)}</div>
      ) : (
        <EmptyState title="Chưa có chuyên gia phù hợp" description="Hãy thay đổi điều kiện tìm kiếm." />
      )}
      <section className="mt-16 bg-slate-100 px-6 py-14 text-center">
        <div className="mx-auto max-w-2xl rounded-2xl border border-line bg-white px-8 py-9">
          <h2 className="font-medium">Bạn chưa biết chọn ai?</h2>
          <p className="mt-3 text-slate-500">Hãy thực hiện bài kiểm tra ngắn để chúng tôi giúp bạn tìm chuyên gia phù hợp nhất với nhu cầu hiện tại của bạn.</p>
          <Link className="mt-6 inline-block" to="/assessments"><Button className="bg-emerald-700 hover:bg-emerald-800" leftIcon={<BrainCircuit className="size-5" />}>Làm bài kiểm tra ngay</Button></Link>
        </div>
      </section>
    </div>
  );
}

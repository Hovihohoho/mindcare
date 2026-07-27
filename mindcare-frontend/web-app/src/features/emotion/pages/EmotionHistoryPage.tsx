import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, PageHeader } from "@/shared";
import { emotionOptions, journalsMock } from "../constants/emotion.constants";

export function EmotionHistoryPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Lịch sử cảm xúc" description="Theo dõi hành trình tâm trí của bạn qua từng ngày." actions={<Link to="/emotion"><Button>Ghi nhật ký hôm nay</Button></Link>} />
      <Card className="p-6">
        <div className="flex items-center justify-between border-b border-line pb-5">
          <Button variant="ghost" size="icon"><ChevronLeft /></Button>
          <div className="flex items-center gap-2 font-extrabold"><CalendarDays className="size-5 text-brand-600" />Tháng 7, 2026</div>
          <Button variant="ghost" size="icon"><ChevronRight /></Button>
        </div>
        <div className="mt-6 grid gap-4">
          {journalsMock.map((entry) => {
            const emotion = emotionOptions.find((item) => item.value === entry.emotion)!;
            return <article className="flex gap-4 rounded-xl border border-line p-4" key={entry.id}><span className="text-3xl">{emotion.emoji}</span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-extrabold">{emotion.label}</h3><Badge tone="neutral">{new Date(entry.recordedAt).toLocaleString("vi-VN")}</Badge></div><p className="mt-2 text-sm leading-6 text-muted">{entry.note}</p></div></article>;
          })}
        </div>
      </Card>
    </div>
  );
}

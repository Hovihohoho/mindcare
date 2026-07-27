import { useState } from "react";
import { Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { Button, Card, Textarea } from "@/shared";
import { EmotionPicker } from "../components/EmotionPicker";
import { EmotionTrendChart } from "../components/EmotionTrendChart";
import type { EmotionLevel } from "../types/emotion.types";

const recent = [
  { emoji: "😁", title: "Rất vui", time: "Hôm qua, 20:30", text: "Hôm nay kết thúc dự án tốt đẹp. Mình thấy rất nhẹ nhõm..." },
  { emoji: "😖", title: "Căng thẳng", time: "14 Tháng 12, 15:45", text: "Nhiều deadline quá, mình cảm thấy hơi ngợp." },
  { emoji: "🙂", title: "Vui", time: "13 Tháng 12, 09:00", text: "Sáng nay dậy sớm tập thể dục, không khí trong lành quá!" },
];

export function EmotionDiaryPage() {
  const [emotion, setEmotion] = useState<EmotionLevel>();
  const [note, setNote] = useState("");
  return (
    <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <div className="space-y-6">
        <Card className="p-8">
          <h1 className="text-3xl font-bold text-slate-900">Chào buổi sáng, &lt;username&gt;</h1>
          <p className="mt-4 text-lg text-slate-500">Hôm nay tâm trạng của bạn như thế nào?</p>
          <div className="mt-8 max-w-[680px]"><EmotionPicker value={emotion} onChange={setEmotion} /></div>
          <div className="mt-8"><Textarea label="Viết thêm về ngày hôm nay của bạn nhé..." placeholder="Hãy chia sẻ những gì đang diễn ra trong đầu bạn..." value={note} onChange={(event) => setNote(event.target.value)} /></div>
          <div className="mt-5 flex justify-end gap-3"><Button variant="outline" onClick={() => { setEmotion(undefined); setNote(""); }}>Hủy</Button><Button>Lưu nhật ký</Button></div>
        </Card>
        <Card className="p-8">
          <div className="flex items-center justify-between"><h2 className="text-2xl font-semibold">Xu hướng cảm xúc trong tuần</h2><span className="rounded-full bg-slate-100 px-4 py-2 text-brand-700">7 ngày qua</span></div>
          <EmotionTrendChart />
          <div className="mt-8 rounded-xl border-l-4 border-brand-600 bg-sky-50 p-5"><p className="flex items-center gap-2 font-semibold text-brand-700"><Sparkles className="size-5" />AI Gợi ý cho bạn</p><p className="mt-2 text-slate-700">Bạn có vẻ đang có một tuần khá tích cực! Việc duy trì thói quen đi dạo vào buổi chiều có thể giúp duy trì chỉ số niềm vui này đấy.</p></div>
        </Card>
      </div>
      <Card className="h-fit p-8">
        <h2 className="text-2xl font-semibold">Lịch sử gần đây</h2>
        <div className="mt-6 divide-y divide-line">{recent.map((item) => <article className="flex gap-4 py-5 first:pt-0" key={item.time}><span className="grid size-12 shrink-0 place-items-center rounded-full bg-emerald-50 text-2xl">{item.emoji}</span><div className="min-w-0"><div className="flex items-center justify-between gap-4"><b>{item.title}</b><span className="whitespace-nowrap text-xs text-muted">{item.time}</span></div><p className="mt-2 line-clamp-2 text-slate-500">{item.text}</p></div></article>)}</div>
        <Link className="mt-6 block text-center text-sm font-semibold text-brand-700" to="/emotion/history">Xem tất cả lịch sử</Link>
      </Card>
    </div>
  );
}

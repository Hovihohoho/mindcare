import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, Card, Textarea } from "@/shared";
import { emotionApi } from "../api/emotion.api";
import { EmotionPicker } from "../components/EmotionPicker";
import { EmotionTrendChart } from "../components/EmotionTrendChart";
import { emotionOptions } from "../constants/emotion.constants";
import type { EmotionLevel } from "../types/emotion.types";

const range = () => ({
  from: new Date(Date.now() - 30 * 86_400_000).toISOString(),
  to: new Date(Date.now() + 86_400_000).toISOString(),
});

export function EmotionDiaryPage() {
  const [emotion, setEmotion] = useState<EmotionLevel>();
  const [content, setContent] = useState("");
  const queryClient = useQueryClient();
  const dates = range();
  const history = useQuery({ queryKey: ["emotion-journals", dates.from, dates.to], queryFn: () => emotionApi.history(dates.from, dates.to) });
  const trends = useQuery({ queryKey: ["emotion-trends", dates.from, dates.to], queryFn: () => emotionApi.trends(dates.from, dates.to) });
  const create = useMutation({
    mutationFn: emotionApi.create,
    onSuccess: async () => {
      setEmotion(undefined);
      setContent("");
      await queryClient.invalidateQueries({ queryKey: ["emotion-journals"] });
      await queryClient.invalidateQueries({ queryKey: ["emotion-trends"] });
    },
  });

  return (
    <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <div className="space-y-6">
        <Card className="p-8">
          <h1 className="text-3xl font-bold text-slate-900">Hôm nay bạn cảm thấy thế nào?</h1>
          <p className="mt-4 text-lg text-slate-500">Ghi lại trạng thái hiện tại một cách chân thật và không phán xét.</p>
          <div className="mt-8 max-w-[680px]"><EmotionPicker value={emotion} onChange={setEmotion} /></div>
          <div className="mt-8"><Textarea label="Điều bạn muốn ghi lại" maxLength={5000} value={content} onChange={(event) => setContent(event.target.value)} /></div>
          {create.isError && <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể lưu nhật ký.</p>}
          <div className="mt-5 flex justify-end gap-3"><Button variant="outline" onClick={() => { setEmotion(undefined); setContent(""); }}>Hủy</Button><Button loading={create.isPending} disabled={!emotion} onClick={() => emotion && create.mutate({ emotionType: emotion, content })}>Lưu nhật ký</Button></div>
        </Card>
        <Card className="p-8">
          <h2 className="text-2xl font-semibold">Xu hướng cảm xúc 30 ngày</h2>
          {trends.isError ? <p className="mt-5 text-rose-700">Không tải được xu hướng.</p> : <EmotionTrendChart points={trends.data ?? []} />}
        </Card>
      </div>
      <Card className="h-fit p-8">
        <h2 className="text-2xl font-semibold">Lịch sử gần đây</h2>
        {history.isLoading && <p className="mt-5 text-muted">Đang tải…</p>}
        {history.isError && <p className="mt-5 text-rose-700">Không tải được nhật ký.</p>}
        <div className="mt-6 divide-y divide-line">{history.data?.items.slice(0, 5).map((item) => { const option = emotionOptions.find((entry) => entry.value === item.emotionType); return <article className="flex gap-4 py-5 first:pt-0" key={item.id}><span className="text-2xl">{option?.emoji}</span><div><b>{option?.label}</b><p className="text-xs text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</p><p className="mt-1 line-clamp-2 text-sm text-slate-500">{item.content || "Không có ghi chú"}</p></div></article>; })}</div>
        <Link className="mt-6 block text-center text-sm font-semibold text-brand-700" to="/emotion/history">Xem tất cả lịch sử</Link>
      </Card>
    </div>
  );
}

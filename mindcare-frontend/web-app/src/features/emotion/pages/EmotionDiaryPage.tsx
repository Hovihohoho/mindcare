import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, Card, Loading, Textarea } from "@/shared";
import { emotionApi } from "../api/emotion.api";
import { EmotionPicker } from "../components/EmotionPicker";
import { EmotionTrendChart } from "../components/EmotionTrendChart";
import { emotionOptions } from "../constants/emotion.constants";
import type { EmotionLevel } from "../types/emotion.types";

const rangeTo = new Date();
const rangeFrom = new Date(rangeTo);
rangeFrom.setDate(rangeFrom.getDate() - 7);

export function EmotionDiaryPage() {
  const queryClient = useQueryClient();
  const [emotion, setEmotion] = useState<EmotionLevel>();
  const [note, setNote] = useState("");
  const history = useQuery({
    queryKey: ["emotion-history", rangeFrom.toISOString(), rangeTo.toISOString()],
    queryFn: () => emotionApi.history(rangeFrom.toISOString(), rangeTo.toISOString()),
  });
  const trends = useQuery({
    queryKey: ["emotion-trends", rangeFrom.toISOString(), rangeTo.toISOString()],
    queryFn: () => emotionApi.trends(rangeFrom.toISOString(), rangeTo.toISOString()),
  });
  const createJournal = useMutation({
    mutationFn: () => emotionApi.create({ emotionType: emotion!, content: note.trim() }),
    onSuccess: async () => {
      setEmotion(undefined);
      setNote("");
      await queryClient.invalidateQueries({ queryKey: ["emotion-history"] });
      await queryClient.invalidateQueries({ queryKey: ["emotion-trends"] });
    },
  });

  return (
    <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
      <div className="space-y-6">
        <Card className="p-8">
          <h1 className="text-3xl font-bold text-slate-900">Nhật ký cảm xúc</h1>
          <p className="mt-4 text-lg text-slate-500">Hôm nay tâm trạng của bạn như thế nào?</p>
          <div className="mt-8 max-w-[680px]"><EmotionPicker value={emotion} onChange={setEmotion} /></div>
          <div className="mt-8">
            <Textarea
              label="Viết thêm về ngày hôm nay"
              placeholder="Hãy chia sẻ những gì đang diễn ra..."
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </div>
          <div className="mt-5 flex justify-end gap-3">
            <Button variant="outline" onClick={() => { setEmotion(undefined); setNote(""); }}>Hủy</Button>
            <Button disabled={!emotion} loading={createJournal.isPending} onClick={() => createJournal.mutate()}>Lưu nhật ký</Button>
          </div>
          {createJournal.isError && <p className="mt-4 text-sm text-rose-700">Không thể lưu nhật ký. Vui lòng thử lại.</p>}
        </Card>
        <Card className="p-8">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-semibold">Xu hướng cảm xúc trong tuần</h2>
            <span className="rounded-full bg-slate-100 px-4 py-2 text-brand-700">7 ngày qua</span>
          </div>
          {trends.isLoading ? <Loading /> : <EmotionTrendChart points={trends.data ?? []} />}
        </Card>
      </div>
      <Card className="h-fit p-8">
        <h2 className="text-2xl font-semibold">Lịch sử gần đây</h2>
        {history.isLoading ? <Loading /> : (
          <div className="mt-6 divide-y divide-line">
            {history.data?.items.slice(0, 5).map((item) => {
              const option = emotionOptions.find((entry) => entry.value === item.emotionType);
              return (
                <article className="flex gap-4 py-5 first:pt-0" key={item.id}>
                  <span className="grid size-12 shrink-0 place-items-center rounded-full bg-emerald-50 text-2xl">{option?.emoji}</span>
                  <div className="min-w-0">
                    <div className="flex items-center justify-between gap-4">
                      <b>{option?.label ?? item.emotionType}</b>
                      <span className="whitespace-nowrap text-xs text-muted">{new Date(item.createdAt).toLocaleString("vi-VN")}</span>
                    </div>
                    <p className="mt-2 line-clamp-2 text-slate-500">{item.content || "Không có ghi chú"}</p>
                  </div>
                </article>
              );
            })}
            {history.data?.items.length === 0 && <p className="py-8 text-center text-sm text-muted">Chưa có nhật ký.</p>}
          </div>
        )}
        <Link className="mt-6 block text-center text-sm font-semibold text-brand-700" to="/emotion/history">Xem tất cả lịch sử</Link>
      </Card>
    </div>
  );
}

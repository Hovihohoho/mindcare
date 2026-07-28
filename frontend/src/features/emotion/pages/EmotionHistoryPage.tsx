import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Loading, PageHeader } from "@/shared";
import { emotionApi } from "../api/emotion.api";
import { emotionOptions } from "../constants/emotion.constants";

export function EmotionHistoryPage() {
  const queryClient = useQueryClient();
  const [now] = useState(() => Date.now());
  const from = new Date(now - 365 * 86_400_000).toISOString();
  const to = new Date(now + 86_400_000).toISOString();
  const history = useQuery({ queryKey: ["emotion-journals", from, to], queryFn: () => emotionApi.history(from, to) });
  const remove = useMutation({
    mutationFn: emotionApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["emotion-journals"] }),
  });

  return (
    <div className="space-y-8">
      <PageHeader title="Lịch sử cảm xúc" description="Theo dõi hành trình tâm trí của bạn qua từng ngày." actions={<Link to="/emotion"><Button>Ghi nhật ký hôm nay</Button></Link>} />
      <Card className="p-6">
        {history.isLoading ? <Loading /> : history.isError ? <p className="text-rose-700">Không tải được lịch sử cảm xúc.</p> : (
          <div className="grid gap-4">
            {history.data?.items.map((entry) => {
              const emotion = emotionOptions.find((item) => item.value === entry.emotionType);
              const canDelete = now - new Date(entry.createdAt).getTime() <= 15 * 60_000;
              return <article className="flex gap-4 rounded-xl border border-line p-4" key={entry.id}><span className="text-3xl">{emotion?.emoji}</span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-extrabold">{emotion?.label}</h3><Badge tone="neutral">{new Date(entry.createdAt).toLocaleString("vi-VN")}</Badge></div><p className="mt-2 text-sm leading-6 text-muted">{entry.content || "Không có ghi chú"}</p>{canDelete && <Button className="mt-3" size="sm" variant="ghost" loading={remove.isPending} onClick={() => remove.mutate(entry.id)}>Xóa</Button>}</div></article>;
            })}
            {!history.data?.items.length && <p className="py-8 text-center text-muted">Chưa có nhật ký cảm xúc.</p>}
          </div>
        )}
      </Card>
    </div>
  );
}

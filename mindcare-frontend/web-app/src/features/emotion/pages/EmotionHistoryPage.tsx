import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Loading, PageHeader } from "@/shared";
import { emotionApi } from "../api/emotion.api";
import { emotionOptions } from "../constants/emotion.constants";

const historyTo = new Date();
const historyFrom = new Date(historyTo);
historyFrom.setFullYear(historyFrom.getFullYear() - 1);

export function EmotionHistoryPage() {
  const history = useQuery({
    queryKey: ["emotion-history", historyFrom.toISOString(), historyTo.toISOString()],
    queryFn: () => emotionApi.history(historyFrom.toISOString(), historyTo.toISOString()),
  });

  return (
    <div className="space-y-8">
      <PageHeader
        title="Lịch sử cảm xúc"
        description="Dữ liệu nhật ký được tải trực tiếp từ Emotion Service."
        actions={<Link to="/emotion"><Button>Ghi nhật ký hôm nay</Button></Link>}
      />
      <Card className="p-6">
        {history.isLoading ? <Loading /> : (
          <div className="grid gap-4">
            {history.data?.items.map((entry) => {
              const emotion = emotionOptions.find((item) => item.value === entry.emotionType);
              return (
                <article className="flex gap-4 rounded-xl border border-line p-4" key={entry.id}>
                  <span className="text-3xl">{emotion?.emoji}</span>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <h3 className="font-extrabold">{emotion?.label ?? entry.emotionType}</h3>
                      <Badge tone="neutral">{new Date(entry.createdAt).toLocaleString("vi-VN")}</Badge>
                    </div>
                    <p className="mt-2 text-sm leading-6 text-muted">{entry.content || "Không có ghi chú"}</p>
                  </div>
                </article>
              );
            })}
            {history.data?.items.length === 0 && <p className="py-10 text-center text-muted">Chưa có dữ liệu nhật ký.</p>}
          </div>
        )}
      </Card>
    </div>
  );
}

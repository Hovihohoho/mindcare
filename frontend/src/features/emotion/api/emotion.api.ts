import { httpClient, type CursorPage } from "@/shared";
import type { EmotionJournal, EmotionLevel, EmotionTrendPoint } from "../types/emotion.types";

export const emotionApi = {
  async create(payload: { emotionType: EmotionLevel; content: string }) {
    const { data } = await httpClient.post<EmotionJournal>("/api/v1/emotion-journals", payload);
    return data;
  },
  async history(from: string, to: string): Promise<CursorPage<EmotionJournal>> {
    const { data } = await httpClient.get<CursorPage<EmotionJournal>>("/api/v1/emotion-journals", { params: { from, to, limit: 30 } });
    return data;
  },
  async trends(from: string, to: string): Promise<EmotionTrendPoint[]> {
    const { data } = await httpClient.get<EmotionTrendPoint[]>("/api/v1/emotion-trends", { params: { from, to, bucket: "DAY", timezone: "Asia/Ho_Chi_Minh" } });
    return data;
  },
  async remove(id: string) {
    await httpClient.delete(`/api/v1/emotion-journals/${id}`);
  },
};

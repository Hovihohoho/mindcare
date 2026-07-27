import { httpClient, type CursorPage } from "@/shared";
import { journalsMock, trendMock } from "../constants/emotion.constants";
import type { EmotionJournal, EmotionLevel, EmotionTrendPoint } from "../types/emotion.types";

const fallbackEnabled = import.meta.env.VITE_ENABLE_API_MOCK_FALLBACK !== "false";

export const emotionApi = {
  async create(payload: { emotion: EmotionLevel; note: string; recordedAt: string }) {
    const { data } = await httpClient.post<EmotionJournal>("/api/v1/emotion-journals", payload);
    return data;
  },
  async history(from: string, to: string): Promise<CursorPage<EmotionJournal>> {
    try {
      const { data } = await httpClient.get<CursorPage<EmotionJournal>>("/api/v1/emotion-journals", { params: { from, to, limit: 30 } });
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      return { items: journalsMock, hasMore: false };
    }
  },
  async trends(from: string, to: string): Promise<EmotionTrendPoint[]> {
    try {
      const { data } = await httpClient.get<EmotionTrendPoint[]>("/api/v1/emotion-trends", { params: { from, to, bucket: "DAY", timezone: "Asia/Ho_Chi_Minh" } });
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      return trendMock;
    }
  },
};

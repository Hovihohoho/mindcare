import type { EmotionJournal, EmotionLevel, EmotionTrendPoint } from "../types/emotion.types";

export const emotionOptions: { value: EmotionLevel; label: string; emoji: string; color: string }[] = [
  { value: "VERY_HAPPY", label: "Rất vui", emoji: "😁", color: "#22c55e" },
  { value: "HAPPY", label: "Vui", emoji: "🙂", color: "#84cc16" },
  { value: "NEUTRAL", label: "Bình thường", emoji: "😐", color: "#facc15" },
  { value: "SAD", label: "Buồn", emoji: "😔", color: "#3b82f6" },
  { value: "STRESSED", label: "Căng thẳng", emoji: "😣", color: "#f43f5e" },
];

export const trendMock: EmotionTrendPoint[] = [3, 4, 3.5, 5, 4.5, 4, 5].map((averageScore, index) => ({
  bucketStart: new Date(2026, 6, 20 + index).toISOString(),
  averageScore,
  journalCount: 1,
}));

export const journalsMock: EmotionJournal[] = [
  { id: "1", emotion: "HAPPY", score: 4, recordedAt: "2026-07-26T15:45:00+07:00", note: "Hoàn thành công việc sớm và có thời gian đi dạo." },
  { id: "2", emotion: "NEUTRAL", score: 3, recordedAt: "2026-07-25T08:30:00+07:00", note: "Một buổi sáng bình thường, ngủ hơi muộn." },
  { id: "3", emotion: "VERY_HAPPY", score: 5, recordedAt: "2026-07-24T19:15:00+07:00", note: "Gặp lại một người bạn cũ sau thời gian dài." },
];

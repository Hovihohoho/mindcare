export type EmotionLevel = "VERY_HAPPY" | "HAPPY" | "NEUTRAL" | "SAD" | "STRESSED";

export interface EmotionJournal {
  id: string;
  emotionType: EmotionLevel;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmotionTrendPoint {
  periodStart: string;
  periodEnd: string;
  averageScore: number;
  count: number;
  mappingVersion: string;
}

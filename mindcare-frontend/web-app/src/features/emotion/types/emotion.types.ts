export type EmotionLevel = "VERY_HAPPY" | "HAPPY" | "NEUTRAL" | "SAD" | "STRESSED";

export interface EmotionJournal {
  id: string;
  emotion: EmotionLevel;
  note: string;
  recordedAt: string;
  score: number;
}

export interface EmotionTrendPoint {
  bucketStart: string;
  averageScore: number;
  journalCount: number;
}

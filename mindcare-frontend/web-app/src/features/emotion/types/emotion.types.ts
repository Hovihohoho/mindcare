export type EmotionLevel = "VERY_HAPPY" | "HAPPY" | "NEUTRAL" | "SAD" | "STRESSED";

export interface EmotionJournal {
  id: string;
  emotionType: EmotionLevel;
  content: string | null;
  energyLevel: number | null;
  stressLevel: number | null;
  sleepQuality: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmotionTrendPoint {
  periodStart: string;
  periodEnd: string;
  averageScore: number | null;
  count: number;
  mappingVersion: string;
}

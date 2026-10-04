export type EmotionLevel = 'VERY_HAPPY' | 'HAPPY' | 'NEUTRAL' | 'SAD' | 'STRESSED';
export type JournalEntrySource = 'USER_DIRECT' | 'MORNING_WELLBEING_PROMPT';

export type EmotionJournal = {
  id: string;
  emotionType: EmotionLevel;
  content: string | null;
  source: JournalEntrySource;
  createdAt: string;
  updatedAt: string;
};

export type EmotionTrendPoint = {
  periodStart: string;
  periodEnd: string;
  averageScore: number | null;
  count: number;
  mappingVersion: string;
};

export type CursorPage<T> = {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type EmotionTone = 'calm' | 'happy' | 'tired' | 'anxious';

export type JournalEntry = {
  id: string;
  title: string;
  note: string;
  createdAt: string;
  tone: EmotionTone;
  score: number;
};

export type Assessment = {
  id: string;
  title: string;
  description: string;
  duration: string;
  questionCount: number;
  category: string;
  completed?: boolean;
};

export type ChatMessage = {
  id: string;
  role: 'assistant' | 'user';
  text: string;
  createdAt: string;
};

export type Expert = {
  id: string;
  name: string;
  role: string;
  specialties: string[];
  experience: number;
  availability: string;
  initials: string;
};

export type SettingItem = {
  id: string;
  title: string;
  description: string;
  icon: 'person' | 'notifications' | 'lock' | 'language' | 'shield';
};

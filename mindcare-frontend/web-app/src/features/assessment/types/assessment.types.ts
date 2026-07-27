export interface AnswerOption {
  id: string;
  label: string;
  value: number;
}

export interface AssessmentQuestion {
  id: string;
  prompt: string;
  options: AnswerOption[];
}

export interface Assessment {
  code: string;
  title: string;
  description: string;
  durationMinutes: number;
  questionCount: number;
  category: string;
  questions?: AssessmentQuestion[];
}

export interface AssessmentResult {
  resultId: string;
  assessmentCode: string;
  score: number;
  severity: "NORMAL" | "MILD" | "MODERATE" | "SEVERE";
  completedAt: string;
}

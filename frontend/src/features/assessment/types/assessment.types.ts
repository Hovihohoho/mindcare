export interface AnswerOption {
  id: string;
  optionText: string;
}

export interface AssessmentQuestion {
  id: string;
  questionText: string;
  orderIndex: number;
  answerOptions: AnswerOption[];
}

export interface Assessment {
  id: string;
  code: string;
  assessmentVersion: number;
  title: string;
  description: string | null;
  questions?: AssessmentQuestion[];
}

export interface AssessmentResult {
  resultId: string;
  assessmentCode: string;
  assessmentVersion: number;
  totalScore: number;
  riskLevel: string;
  screeningNotice: string;
  recommendations: string[];
  createdAt: string;
}

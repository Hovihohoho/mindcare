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
  description: string;
  evidence: {
    publisher: string;
    sourceTitle: string;
    sourceUrl: string;
    publicationYear: number;
    instrumentVersion: string;
    license: string;
    scoringRuleVersion: string;
    purpose: string;
    limitation: string;
  };
  questions?: AssessmentQuestion[];
}

export interface AssessmentResult {
  resultId: string;
  assessmentCode: string;
  assessmentVersion: number;
  totalScore: number;
  riskLevel: InterpretationLevel;
  normalizedScore?: number | null;
  interpretationLevel: InterpretationLevel;
  scoringPolicyKey?: string | null;
  scoringPolicyVersion?: string | null;
  benchmarkPolicyKey?: string | null;
  benchmarkPolicyVersion?: string | null;
  riskSignals: Array<{ type: string; reasonCode: string; responseValue: number; ruleVersion: string }>;
  screeningNotice: string;
  recommendations: string[];
  createdAt: string;
}

export type InterpretationLevel = "MINIMAL" | "MILD" | "MODERATE" | "MODERATELY_SEVERE" |
  "SEVERE" | "LOW_WELL_BEING" | "ADEQUATE_WELL_BEING" | "TRACKING_ONLY" | "NORMAL" | "EXTREME";

export interface RiskAlert {
  id: string;
  alertLevel: "ELEVATED" | "HIGH";
  triggerReason: string;
  ruleVersion: string;
  reasonCode: string;
  sourceResultId: string;
  notified: boolean;
  createdAt: string;
}

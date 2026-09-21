import type { AssessmentResult, InterpretationLevel } from "../types/assessment.types";

export const riskLevelLabel: Record<InterpretationLevel, string> = {
  MINIMAL: "Triệu chứng tối thiểu",
  NORMAL: "Trong ngưỡng bình thường",
  MILD: "Triệu chứng nhẹ",
  MODERATE: "Triệu chứng trung bình",
  MODERATELY_SEVERE: "Triệu chứng khá nặng",
  SEVERE: "Triệu chứng nặng",
  EXTREME: "Mức rất cao",
  LOW_WELL_BEING: "Well-being thấp – nên đánh giá thêm",
  ADEQUATE_WELL_BEING: "Well-being ổn định",
  TRACKING_ONLY: "Chỉ số theo dõi xu hướng",
};

export function resultInterpretation(result: AssessmentResult): InterpretationLevel {
  return result.interpretationLevel ?? result.riskLevel;
}

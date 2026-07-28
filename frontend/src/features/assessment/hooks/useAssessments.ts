import { useQuery } from "@tanstack/react-query";
import { assessmentApi } from "../api/assessment.api";

export function useAssessments() {
  return useQuery({ queryKey: ["assessments"], queryFn: assessmentApi.list });
}

export function useAssessment(code: string) {
  return useQuery({ queryKey: ["assessments", code], queryFn: () => assessmentApi.detail(code) });
}

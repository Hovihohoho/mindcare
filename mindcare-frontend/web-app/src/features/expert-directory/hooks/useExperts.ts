import { useQuery } from "@tanstack/react-query";
import { expertApi } from "../api/expert.api";

export function useExperts(keyword: string, specialty: string) {
  return useQuery({ queryKey: ["experts", keyword, specialty], queryFn: () => expertApi.list({ keyword: keyword || undefined, specialty: specialty || undefined }) });
}

export function useExpert(id: string) {
  return useQuery({ queryKey: ["experts", id], queryFn: () => expertApi.getById(id), enabled: Boolean(id) });
}

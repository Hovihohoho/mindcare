import { httpClient } from "@/shared";

export type CareGoal = "REDUCE_STRESS" | "IMPROVE_SLEEP" | "MANAGE_ANXIETY" | "BUILD_BALANCE";
export interface CareActivity {
  id: string;
  activityCode: string;
  title: string;
  targetPerWeek: number;
  completedThisWeek: number;
  completedToday: boolean;
}
export interface CarePlan {
  id: string;
  goal: CareGoal;
  weekStartedOn: string;
  completedThisWeek: number;
  targetThisWeek: number;
  activities: CareActivity[];
  updatedAt: string;
}
export interface CareActivityInput { activityCode: string; title: string; targetPerWeek: number }

export const carePlanApi = {
  async get() { const { data } = await httpClient.get<CarePlan>("/api/v1/self-care-plan"); return data; },
  async save(goal: CareGoal, activities: CareActivityInput[]) {
    const { data } = await httpClient.put<CarePlan>("/api/v1/self-care-plan", { goal, activities }); return data;
  },
  async complete(activityId: string, completedOn: string) {
    const { data } = await httpClient.post<CarePlan>(`/api/v1/self-care-plan/activities/${activityId}/completions`, { completedOn }); return data;
  },
  async undo(activityId: string, completedOn: string) {
    const { data } = await httpClient.delete<CarePlan>(`/api/v1/self-care-plan/activities/${activityId}/completions/${completedOn}`); return data;
  },
};

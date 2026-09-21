import { apiRequest } from '@/services/api/api.client';

export type CareGoal = 'REDUCE_STRESS' | 'IMPROVE_SLEEP' | 'MANAGE_ANXIETY' | 'BUILD_BALANCE';
export type CareActivity = { id: string; activityCode: string; title: string; targetPerWeek: number; completedThisWeek: number; completedToday: boolean };
export type CarePlan = { id: string; goal: CareGoal; templateCode: string | null; templateVersion: string | null; sourceUrl: string | null; weekStartedOn: string; completedThisWeek: number; targetThisWeek: number; activities: CareActivity[]; updatedAt: string };
export type CarePlanTemplate = { templateCode: string; templateVersion: string; goal: CareGoal; title: string; description: string; activities: { activityCode: string; title: string; targetPerWeek: number }[]; sourceTitle: string; sourceUrl: string; implementationSourceUrl: string; limitation: string };
export type CarePlanRecommendation = { templateCode: string; message: string; benchmarkSourceUrl: string };

export const carePlanService = {
  get(token: string) { return apiRequest<CarePlan>('/api/v1/self-care-plan', { token, responseType: 'raw' }); },
  templates(token: string) { return apiRequest<CarePlanTemplate[]>('/api/v1/self-care-plan/templates', { token, responseType: 'raw' }); },
  recommendations(token: string) { return apiRequest<CarePlanRecommendation[]>('/api/v1/self-care-plan/recommendations', { token, responseType: 'raw' }); },
  applyTemplate(token: string, templateCode: string) { return apiRequest<CarePlan>(`/api/v1/self-care-plan/templates/${templateCode}:apply`, { method: 'POST', responseType: 'raw', token }); },
  complete(token: string, activityId: string, completedOn: string) { return apiRequest<CarePlan>(`/api/v1/self-care-plan/activities/${activityId}/completions`, { body: { completedOn }, method: 'POST', responseType: 'raw', token }); },
  undo(token: string, activityId: string, completedOn: string) { return apiRequest<CarePlan>(`/api/v1/self-care-plan/activities/${activityId}/completions/${completedOn}`, { method: 'DELETE', responseType: 'raw', token }); },
};

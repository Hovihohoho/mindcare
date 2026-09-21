import { httpClient, type ApiResponse } from "@/shared";
export type ReminderType = "DAILY_CHECK_IN" | "SELF_CARE";
export interface ReminderPreference { id: string; reminderType: ReminderType; enabled: boolean; localTime: string; timezone: string }
export const reminderApi = {
  async list() { const { data } = await httpClient.get<ApiResponse<ReminderPreference[]>>("/api/v1/reminders"); return data.data; },
  async update(type: ReminderType, input: Pick<ReminderPreference, "enabled" | "localTime" | "timezone">) { const { data } = await httpClient.put<ApiResponse<ReminderPreference>>(`/api/v1/reminders/${type}`, input); return data.data; },
};

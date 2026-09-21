import { httpClient, type ApiResponse } from "@/shared";

export interface SelfCareContent {
  id: string;
  title: string;
  content: string;
  sourceUrl: string;
  publisher?: string;
  evidenceScope?: string;
  limitation?: string;
  updatedAt: string;
}

export const selfCareApi = {
  async list() {
    const { data } = await httpClient.get<ApiResponse<SelfCareContent[]>>("/api/ai/self-care-content");
    return data.data;
  },
  async find(id: string) {
    const { data } = await httpClient.get<ApiResponse<SelfCareContent>>(`/api/ai/self-care-content/${id}`);
    return data.data;
  },
};

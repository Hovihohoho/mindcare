import { httpClient, type ApiResponse } from "@/shared";

export const privacyApi = {
  async exportEmotionData() {
    const { data } = await httpClient.get<Record<string, unknown>>("/api/v1/privacy/export");
    return data;
  },
  async exportAiData() {
    const { data } = await httpClient.get<ApiResponse<Record<string, unknown>>>("/api/ai/privacy/export");
    return data.data;
  },
  async deleteEmotionData() {
    await httpClient.delete("/api/v1/privacy/data");
  },
  async deleteAiData() {
    await httpClient.delete("/api/ai/privacy/data");
  },
};

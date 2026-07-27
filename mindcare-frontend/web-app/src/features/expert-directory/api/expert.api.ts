import { httpClient, type CursorPage } from "@/shared";
import { expertMocks } from "../constants/expert.mock";
import type { ExpertSummary } from "../types/expert.types";

const fallbackEnabled = import.meta.env.VITE_ENABLE_API_MOCK_FALLBACK !== "false";

export const expertApi = {
  async list(params?: { keyword?: string; specialty?: string }): Promise<CursorPage<ExpertSummary>> {
    try {
      const { data } = await httpClient.get<CursorPage<ExpertSummary>>("/api/v1/experts", { params });
      return data;
    } catch (error) {
      if (!fallbackEnabled) throw error;
      const keyword = params?.keyword?.toLocaleLowerCase();
      const items = expertMocks.filter((expert) => (!keyword || expert.displayName.toLocaleLowerCase().includes(keyword)) && (!params?.specialty || expert.specialties.includes(params.specialty)));
      return { items, hasMore: false };
    }
  },
  async getById(id: string) {
    const page = await this.list();
    return page.items.find((expert) => expert.expertUserId === id) ?? expertMocks[0];
  },
};

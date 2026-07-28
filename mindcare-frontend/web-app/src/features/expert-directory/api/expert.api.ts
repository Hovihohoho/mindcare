import { httpClient, type CursorPage } from "@/shared";
import type { ExpertSummary } from "../types/expert.types";

export const expertApi = {
  async list(params?: { keyword?: string; specialty?: string }): Promise<CursorPage<ExpertSummary>> {
    const { data } = await httpClient.get<CursorPage<ExpertSummary>>("/api/v1/experts", { params });
    return data;
  },
  async getById(id: string) {
    const page = await this.list();
    const expert = page.items.find((item) => item.expertUserId === id);
    if (!expert) throw new Error("Expert not found");
    return expert;
  },
};

import { httpClient, type ApiResponse } from "@/shared";

export type BookmarkType = "ASSESSMENT";
export interface BookmarkItem {
  id: string;
  targetType: BookmarkType;
  targetId: string;
  createdAt: string;
}

export const bookmarkApi = {
  async list() {
    const { data } = await httpClient.get<ApiResponse<BookmarkItem[]>>("/api/v1/bookmarks");
    return data.data;
  },
  async add(targetType: BookmarkType, targetId: string) {
    await httpClient.post("/api/v1/bookmarks", { targetType, targetId });
  },
  async remove(targetType: BookmarkType, targetId: string) {
    await httpClient.delete("/api/v1/bookmarks", { params: { targetType, targetId } });
  },
};

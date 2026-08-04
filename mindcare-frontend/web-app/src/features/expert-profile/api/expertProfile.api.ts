import { httpClient, type ApiResponse } from "@/shared";
import type { AuthUser, UpdateProfilePayload } from "@/features/auth";

export interface ExpertDocument {
  id: string;
  documentType: string;
  title: string;
  fileUrl: string;
  createdAt: string;
}
export interface ExpertAccountProfile { profile: AuthUser; documents: ExpertDocument[] }

export const expertProfileApi = {
  async me() {
    const { data } = await httpClient.get<ApiResponse<ExpertAccountProfile>>("/api/auth/expert-profile/me");
    return data.data;
  },
  async update(payload: UpdateProfilePayload) {
    const { data } = await httpClient.put<ApiResponse<AuthUser>>("/api/auth/me", payload);
    return data.data;
  },
  async addDocument(payload: Omit<ExpertDocument, "id" | "createdAt">) {
    const { data } = await httpClient.post<ApiResponse<ExpertDocument>>("/api/auth/expert-profile/documents", payload);
    return data.data;
  },
  async uploadDocument(file: File, documentType: string, title: string) {
    const form = new FormData();
    form.append("file", file);
    form.append("documentType", documentType);
    form.append("title", title);
    const { data } = await httpClient.post<ApiResponse<ExpertDocument>>(
      "/api/auth/expert-profile/documents/upload",
      form,
      { headers: { "Content-Type": "multipart/form-data" } },
    );
    return data.data;
  },
  async openDocument(id: string) {
    const { data } = await httpClient.get<Blob>(
      `/api/auth/expert-profile/documents/${id}/file`,
      { responseType: "blob" },
    );
    const url = URL.createObjectURL(data);
    window.open(url, "_blank", "noopener,noreferrer");
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  },
  async deleteDocument(id: string) {
    await httpClient.delete(`/api/auth/expert-profile/documents/${id}`);
  },
  async submit() {
    const { data } = await httpClient.post<ApiResponse<ExpertAccountProfile>>("/api/auth/expert-profile/submit");
    return data.data;
  },
};

import type { ApiResponse } from "@/shared";
import { httpClient } from "@/shared";
import type { AdminUser, KnowledgeDocument, Role } from "../types/admin.types";

export const adminApi = {
  async users() {
    const { data } = await httpClient.get<ApiResponse<AdminUser[]>>("/api/auth/admin/users");
    return data.data;
  },
  async roles() {
    const { data } = await httpClient.get<ApiResponse<Role[]>>("/api/auth/admin/users/roles");
    return data.data;
  },
  async createExpert(payload: { fullName: string; email: string; password: string }) {
    const { data } = await httpClient.post<ApiResponse<AdminUser>>("/api/auth/admin/users", payload);
    return data.data;
  },
  async updateUser(id: string, payload: { fullName: string; email: string; role: string; active: boolean }) {
    const { data } = await httpClient.put<ApiResponse<AdminUser>>(`/api/auth/admin/users/${id}`, payload);
    return data.data;
  },
  async deleteUser(id: string) {
    await httpClient.delete(`/api/auth/admin/users/${id}`);
  },
  async documents() {
    const { data } = await httpClient.get<ApiResponse<KnowledgeDocument[]>>("/api/ai/documents");
    return data.data;
  },
  async createDocument(payload: Omit<KnowledgeDocument, "id" | "createdAt" | "updatedAt">) {
    const { data } = await httpClient.post<ApiResponse<KnowledgeDocument>>("/api/ai/documents", payload);
    return data.data;
  },
  async updateDocument(id: string, payload: Omit<KnowledgeDocument, "id" | "createdAt" | "updatedAt">) {
    const { data } = await httpClient.put<ApiResponse<KnowledgeDocument>>(`/api/ai/documents/${id}`, payload);
    return data.data;
  },
  async deleteDocument(id: string) {
    await httpClient.delete(`/api/ai/documents/${id}`);
  },
  async reindexDocument(id: string) {
    await httpClient.post(`/api/ai/documents/${id}/reindex`);
  },
  async reindexAll() {
    const { data } = await httpClient.post<ApiResponse<number>>("/api/ai/documents/reindex-all");
    return data.data;
  },
  async broadcast(payload: { title: string; content: string; actionUrl?: string | null }) {
    const { data } = await httpClient.post<ApiResponse<number>>("/api/auth/notifications/admin/broadcast", payload);
    return data.data;
  },
};

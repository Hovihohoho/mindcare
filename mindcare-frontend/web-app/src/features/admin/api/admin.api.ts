import { httpClient, type ApiResponse } from "@/shared";
import type { AuthUser, UserRole } from "@/features/auth";
import type { ExpertAccountProfile } from "@/features/expert-profile/api/expertProfile.api";

export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface DashboardStats { totalUsers: number; activeUsers: number; experts: number; pendingExperts: number }
export interface AuditLog { id: string; adminEmail?: string; action: string; targetType: string; targetId?: string; detail?: string; createdAt: string }
export interface KnowledgeDocument {
  id: string;
  title: string;
  content: string;
  sourceUrl?: string;
  documentType?: string;
  active: boolean;
  originalFilename?: string;
  mimeType?: string;
  fileSize?: number;
  processingStatus?: "PROCESSING" | "READY" | "FAILED";
  processingError?: string;
  indexedAt?: string;
}
export interface AssessmentAdmin { id: string; code: string; title: string; status: string; version: number }

export const adminApi = {
  async dashboard() {
    const { data } = await httpClient.get<ApiResponse<DashboardStats>>("/api/auth/admin/management/dashboard");
    return data.data;
  },
  async users(search = "", page = 0) {
    const { data } = await httpClient.get<ApiResponse<Page<AuthUser>>>("/api/auth/admin/management/users", { params: { search, page, size: 20 } });
    return data.data;
  },
  async setActive(id: string, active: boolean) {
    const { data } = await httpClient.patch<ApiResponse<AuthUser>>(`/api/auth/admin/management/users/${id}/active`, { active });
    return data.data;
  },
  async setRole(id: string, role: UserRole) {
    const { data } = await httpClient.patch<ApiResponse<AuthUser>>(`/api/auth/admin/management/users/${id}/role`, { role });
    return data.data;
  },
  async deleteUser(id: string) {
    await httpClient.delete(`/api/auth/admin/management/users/${id}`);
  },
  async experts(status = "PENDING", page = 0) {
    const { data } = await httpClient.get<ApiResponse<Page<AuthUser>>>("/api/auth/admin/management/experts", { params: { status, page, size: 20 } });
    return data.data;
  },
  async reviewExpert(id: string, status: "APPROVED" | "REJECTED", reason: string) {
    const { data } = await httpClient.patch<ApiResponse<AuthUser>>(`/api/auth/admin/management/experts/${id}/review`, { status, reason });
    return data.data;
  },
  async expert(id: string) {
    const { data } = await httpClient.get<ApiResponse<ExpertAccountProfile>>(`/api/auth/admin/management/experts/${id}`);
    return data.data;
  },
  async openExpertDocument(id: string) {
    const { data } = await httpClient.get<Blob>(`/api/auth/expert-profile/documents/${id}/file`, { responseType: "blob" });
    const url = URL.createObjectURL(data);
    window.open(url, "_blank", "noopener,noreferrer");
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  },
  async audit(page = 0) {
    const { data } = await httpClient.get<ApiResponse<Page<AuditLog>>>("/api/auth/admin/management/audit-logs", { params: { page, size: 30 } });
    return data.data;
  },
  async broadcast(title: string, content: string, actionUrl?: string) {
    const { data } = await httpClient.post<ApiResponse<number>>("/api/auth/notifications/admin/broadcast", { title, content, actionUrl });
    return data.data;
  },
  async documents() {
    const { data } = await httpClient.get<ApiResponse<KnowledgeDocument[]>>("/api/ai/documents");
    return data.data;
  },
  async createDocument(payload: Omit<KnowledgeDocument, "id">) {
    const { data } = await httpClient.post<ApiResponse<KnowledgeDocument>>("/api/ai/documents", payload);
    return data.data;
  },
  async uploadDocument(file: File, title?: string, sourceUrl?: string) {
    const form = new FormData();
    form.append("file", file);
    if (title) form.append("title", title);
    if (sourceUrl) form.append("sourceUrl", sourceUrl);
    form.append("documentType", "KNOWLEDGE");
    const { data } = await httpClient.post<ApiResponse<KnowledgeDocument>>("/api/ai/documents/upload", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data.data;
  },
  async deleteDocument(id: string) { await httpClient.delete(`/api/ai/documents/${id}`); },
  async reindexDocument(id: string) { await httpClient.post(`/api/ai/documents/${id}/reindex`); },
  async assessments() {
    const { data } = await httpClient.get<{ items: AssessmentAdmin[] }>("/api/v1/admin/assessments", { params: { limit: 100 } });
    return data.items;
  },
  async publishAssessment(id: string) { await httpClient.post(`/api/v1/admin/assessments/${id}:publish`); },
  async archiveAssessment(id: string) { await httpClient.post(`/api/v1/admin/assessments/${id}:archive`); },
};

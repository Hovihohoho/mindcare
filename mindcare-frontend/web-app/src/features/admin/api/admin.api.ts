import { httpClient, type ApiResponse } from "@/shared";
import type { AuthUser, UserRole } from "@/features/auth";

export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface DashboardStats { totalUsers: number; activeUsers: number }
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
  processingStatus?: "PENDING_REVIEW" | "PROCESSING" | "READY" | "FAILED" | "REJECTED";
  processingError?: string;
  indexedAt?: string;
  publisher?: string;
  publicationYear?: number;
  sourceTier: "A" | "B" | "C" | "D" | "UNRATED";
  reviewStatus: "DRAFT" | "NEEDS_REVIEW" | "APPROVED" | "REJECTED" | "EXPIRED";
  reviewedBy?: string;
  reviewedAt?: string;
  evidenceScope?: string;
  limitation?: string;
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
  async createDocument(payload: { title: string; content: string; sourceUrl: string; documentType?: string; active?: boolean }) {
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
  async enableDocument(id: string) { await httpClient.post(`/api/ai/documents/${id}/enable`); },
  async disableDocument(id: string) { await httpClient.post(`/api/ai/documents/${id}/reject`); },
  async assessments() {
    const { data } = await httpClient.get<{ items: AssessmentAdmin[] }>("/api/v1/admin/assessments", { params: { limit: 100 } });
    return data.items;
  },
  async publishAssessment(id: string) { await httpClient.post(`/api/v1/admin/assessments/${id}:publish`); },
  async archiveAssessment(id: string) { await httpClient.post(`/api/v1/admin/assessments/${id}:archive`); },
};

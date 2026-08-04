import { httpClient, type ApiResponse } from "@/shared";
import type { AuthSession, AuthUser, LoginPayload, LoginSession, RegisterPayload, UpdateProfilePayload } from "../types/auth.types";

export const authApi = {
  async login(payload: LoginPayload) {
    const { data } = await httpClient.post<ApiResponse<AuthSession>>("/api/auth/login", payload);
    return data.data;
  },
  async register(payload: RegisterPayload) {
    // Real SMTP can take longer than the default API timeout while Gmail
    // accepts the message. Keep this request alive long enough to receive the
    // authoritative registration result from auth-service.
    const { data } = await httpClient.post<ApiResponse<null>>("/api/auth/register", payload, { timeout: 45_000 });
    return data.data;
  },
  async me() {
    const { data } = await httpClient.get<ApiResponse<AuthUser>>("/api/auth/me");
    return data.data;
  },
  async updateMe(payload: UpdateProfilePayload) {
    const { data } = await httpClient.put<ApiResponse<AuthUser>>("/api/auth/me", payload);
    return data.data;
  },
  async uploadAvatar(file: File) {
    const form = new FormData();
    form.append("file", file);
    const { data } = await httpClient.post<ApiResponse<AuthUser>>("/api/auth/me/avatar", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data.data;
  },
  async changePassword(currentPassword: string, newPassword: string) {
    await httpClient.put("/api/auth/me/password", { currentPassword, newPassword });
  },
  async deactivate() {
    await httpClient.delete("/api/auth/me");
  },
  async sessions() {
    const { data } = await httpClient.get<ApiResponse<LoginSession[]>>("/api/auth/sessions");
    return data.data;
  },
  async revokeSession(id: string) {
    await httpClient.delete(`/api/auth/sessions/${id}`);
  },
  async logout() {
    await httpClient.post("/api/auth/logout");
  },
  async forgotPassword(email: string) {
    await httpClient.post("/api/auth/forgot-password", { email }, { timeout: 45_000 });
  },
  async resetPassword(token: string, newPassword: string) {
    await httpClient.post("/api/auth/reset-password", { token, newPassword });
  },
  async verifyEmail(email: string, code: string) {
    await httpClient.post("/api/auth/verify-email", { email, code });
  },
  async resendVerification(email: string) {
    await httpClient.post("/api/auth/resend-verification", { email }, { timeout: 45_000 });
  },
};

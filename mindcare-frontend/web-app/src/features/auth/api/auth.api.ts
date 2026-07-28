import { httpClient, type ApiResponse } from "@/shared";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload, UpdateProfilePayload } from "../types/auth.types";

export const authApi = {
  async login(payload: LoginPayload) {
    const { data } = await httpClient.post<ApiResponse<AuthSession>>("/api/auth/login", payload);
    return data.data;
  },
  async register(payload: RegisterPayload) {
    const { data } = await httpClient.post<ApiResponse<null>>("/api/auth/register", payload);
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
  async verifyEmail(email: string, code: string) {
    await httpClient.post("/api/auth/verify-email", { email, code });
  },
  async resendVerification(email: string) {
    await httpClient.post("/api/auth/resend-verification", { email });
  },
};

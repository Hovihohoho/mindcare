import { httpClient, type ApiResponse } from "@/shared";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload } from "../types/auth.types";

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
  async verifyEmail(token: string) {
    await httpClient.get("/api/auth/verify-email", { params: { token } });
  },
  async resendVerification(email: string) {
    await httpClient.post("/api/auth/resend-verification", { email });
  },
};

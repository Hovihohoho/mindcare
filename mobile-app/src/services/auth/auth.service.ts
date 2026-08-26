import type { AuthSession, LoginInput, LoginSession, RegisterInput } from '@/features/auth/auth.types';
import { apiRequest, ApiClientError } from '@/services/api/api.client';

export class AuthServiceError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = 'AuthServiceError';
  }
}

export type AuthService = {
  login(input: LoginInput): Promise<AuthSession>;
  register(input: RegisterInput): Promise<void>;
  verifyEmail(email: string, code: string): Promise<void>;
  resendVerification(email: string): Promise<void>;
  me(accessToken: string): Promise<AuthSession['user']>;
  logout(accessToken?: string): Promise<void>;
  sessions(accessToken: string): Promise<LoginSession[]>;
  exportMyData(accessToken: string): Promise<Record<string, unknown>>;
  verifyPassword(accessToken: string, currentPassword: string): Promise<void>;
  permanentlyDelete(accessToken: string, currentPassword: string): Promise<void>;
};

async function authRequest<T>(path: string, options: Parameters<typeof apiRequest<T>>[1]): Promise<T> {
  try {
    return await apiRequest<T>(path, options);
  } catch (error) {
    if (error instanceof ApiClientError) throw new AuthServiceError(error.message, error.status);
    throw error;
  }
}

export const authService: AuthService = {
  async login(input) {
    const session = await authRequest<Omit<AuthSession, 'expiresAt'>>('/api/auth/login', { method: 'POST', body: input });
    return { ...session, expiresAt: Date.now() + session.expiresIn * 1000 };
  },

  async register(input) {
    await authRequest<null>('/api/auth/register', { method: 'POST', body: input, timeout: 45_000 });
  },

  async verifyEmail(email, code) {
    await authRequest<null>('/api/auth/verify-email', { method: 'POST', body: { email, code } });
  },

  async resendVerification(email) {
    await authRequest<null>('/api/auth/resend-verification', { method: 'POST', body: { email }, timeout: 45_000 });
  },

  async me(accessToken) {
    return authRequest<AuthSession['user']>('/api/auth/me', { token: accessToken });
  },

  async logout(accessToken) {
    if (!accessToken) return;
    await authRequest<null>('/api/auth/logout', { method: 'POST', token: accessToken });
  },

  async sessions(accessToken) {
    return authRequest<LoginSession[]>('/api/auth/sessions', { token: accessToken });
  },

  async exportMyData(accessToken) {
    return authRequest<Record<string, unknown>>('/api/auth/me/data-export', { token: accessToken });
  },

  async verifyPassword(accessToken, currentPassword) {
    await authRequest<null>('/api/auth/me/verify-password', { body: { currentPassword }, method: 'POST', token: accessToken });
  },

  async permanentlyDelete(accessToken, currentPassword) {
    await authRequest<null>('/api/auth/me/permanent', { body: { currentPassword }, method: 'DELETE', token: accessToken });
  },
};

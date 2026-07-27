export type UserRole = "USER" | "EXPERT" | "ADMIN";

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  roles: UserRole[];
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  fullName: string;
}

export interface AuthSession {
  accessToken: string;
  user: AuthUser;
}

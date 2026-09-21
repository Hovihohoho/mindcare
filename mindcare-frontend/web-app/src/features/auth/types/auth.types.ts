export type UserRole = "ROLE_USER" | "ROLE_ADMIN";

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  avatarUrl?: string | null;
  phone?: string | null;
  birthDate?: string | null;
  gender?: string | null;
  address?: string | null;
  bio?: string | null;
}

export type UpdateProfilePayload = Pick<AuthUser, "fullName"> &
  Partial<Pick<AuthUser, "phone" | "birthDate" | "gender" | "address" | "bio">>;

export interface LoginSession {
  id: string;
  userAgent?: string;
  ipAddress?: string;
  createdAt: string;
  lastSeenAt: string;
  expiresAt: string;
  revoked: boolean;
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
  tokenType: "Bearer";
  expiresIn: number;
  user: AuthUser;
}

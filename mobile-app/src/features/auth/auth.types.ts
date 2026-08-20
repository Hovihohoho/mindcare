export type AuthUser = {
  id: string;
  email: string;
  fullName: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  avatarUrl?: string | null;
  phone?: string | null;
  birthDate?: string | null;
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | null;
  address?: string | null;
  bio?: string | null;
};

export type LoginSession = {
  id: string;
  userAgent: string | null;
  ipAddress: string | null;
  createdAt: string;
  lastSeenAt: string;
  expiresAt: string;
  revoked: boolean;
};

export type AuthSession = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  expiresAt: number;
  user: AuthUser;
};

export type LoginInput = {
  email: string;
  password: string;
};

export type RegisterInput = LoginInput & {
  fullName: string;
};

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

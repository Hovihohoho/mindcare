export type UserRole = "ROLE_USER" | "ROLE_EXPERT" | "ROLE_ADMIN";

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  phone?: string | null;
  birthDate?: string | null;
  gender?: string | null;
  address?: string | null;
  bio?: string | null;
  headline?: string | null;
  specialties?: string | null;
  yearsOfExperience?: number | null;
  consultationFee?: number | null;
  workplace?: string | null;
  education?: string | null;
}

export type UpdateProfilePayload = Pick<AuthUser, "fullName"> &
  Partial<Pick<AuthUser, "phone" | "birthDate" | "gender" | "address" | "bio" | "headline" | "specialties" | "yearsOfExperience" | "consultationFee" | "workplace" | "education">>;

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

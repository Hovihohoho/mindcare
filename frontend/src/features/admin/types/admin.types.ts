import type { AuthUser, UserRole } from "@/features/auth";

export type AdminUser = AuthUser;

export interface Role {
  id: number;
  name: UserRole;
  description: string;
}

export interface KnowledgeDocument {
  id: string;
  title: string;
  content: string;
  sourceUrl: string | null;
  documentType: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import type { AuthUser, UserRole } from "../types/auth.types";
import { tokenStorage, userStorage } from "@/shared/lib/storage";

export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const location = useLocation();
  const user = userStorage.get<AuthUser>();
  if (!tokenStorage.get() || !user) {
    return <Navigate replace state={{ from: location.pathname }} to="/login" />;
  }
  if (!roles.includes(user.role)) {
    return <Navigate replace to="/forbidden" />;
  }
  return children;
}

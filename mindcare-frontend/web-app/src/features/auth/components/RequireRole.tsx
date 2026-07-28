import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { tokenStorage, userStorage } from "@/shared/lib/storage";
import type { AuthUser, UserRole } from "../types/auth.types";

interface RequireRoleProps {
  roles: UserRole[];
  children: ReactNode;
}

export function RequireRole({ roles, children }: RequireRoleProps) {
  const location = useLocation();
  const token = tokenStorage.get();
  const user = userStorage.get<AuthUser>();

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (!roles.includes(user.role)) {
    const fallback =
      user.role === "ROLE_ADMIN"
        ? "/admin"
        : user.role === "ROLE_EXPERT"
          ? "/expert"
          : "/";
    return <Navigate to={fallback} replace />;
  }

  return children;
}

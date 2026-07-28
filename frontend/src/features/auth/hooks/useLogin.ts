import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { tokenStorage, userStorage } from "@/shared/lib/storage";
import { authApi } from "../api/auth.api";

export function useLogin() {
  const navigate = useNavigate();
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (session) => {
      tokenStorage.set(session.accessToken);
      userStorage.set(session.user);
      navigate(
        session.user.role === "ROLE_ADMIN"
          ? "/admin"
          : session.user.role === "ROLE_EXPERT"
            ? "/expert"
            : "/",
      );
    },
  });
}

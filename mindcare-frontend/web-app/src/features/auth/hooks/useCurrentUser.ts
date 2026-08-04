import { useQuery } from "@tanstack/react-query";
import { tokenStorage } from "@/shared/lib/storage";
import { authApi } from "../api/auth.api";

export function useCurrentUser() {
  return useQuery({
    queryKey: ["current-user"],
    queryFn: authApi.me,
    enabled: Boolean(tokenStorage.get()),
    retry: false,
  });
}

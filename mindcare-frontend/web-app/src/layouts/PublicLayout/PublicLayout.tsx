import { Navigate, Outlet } from "react-router-dom";
import { useCurrentUser } from "@/features/auth";
import { Loading } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";
import { UserFooter } from "../components/UserFooter";
import { UserHeader } from "../components/UserHeader";

export function PublicLayout() {
  const currentUser = useCurrentUser();
  const hasStoredSession = Boolean(tokenStorage.get());

  if (hasStoredSession && currentUser.isLoading) {
    return <div className="grid min-h-screen place-items-center"><Loading /></div>;
  }

  if (currentUser.data?.role === "ROLE_EXPERT") {
    return <Navigate to="/expert" replace />;
  }

  return (
    <div className="min-h-screen bg-white">
      <UserHeader />
      <main><Outlet /></main>
      <UserFooter />
    </div>
  );
}

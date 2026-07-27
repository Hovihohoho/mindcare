import { Outlet } from "react-router-dom";
import { UserFooter } from "../components/UserFooter";
import { UserHeader } from "../components/UserHeader";

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-white">
      <UserHeader />
      <main><Outlet /></main>
      <UserFooter />
    </div>
  );
}

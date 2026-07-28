import { Outlet } from "react-router-dom";

export function UserLayout() {
  return <div className="page-container min-h-[calc(100vh-237px)] py-10 lg:py-12"><Outlet /></div>;
}

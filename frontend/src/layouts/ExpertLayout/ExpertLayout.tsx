import { Bell, LogOut } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import type { AuthUser } from "@/features/auth";
import { Avatar, Logo, cn } from "@/shared";
import { expertNavigation, expertUtilityNavigation } from "@/shared/constants/navigation";
import { tokenStorage, userStorage } from "@/shared/lib/storage";

const navClass = ({ isActive }: { isActive: boolean }) =>
  cn("flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition", isActive ? "bg-emerald-300 text-slate-700" : "text-slate-600 hover:bg-slate-50");

export function ExpertLayout() {
  const navigate = useNavigate();
  const user = userStorage.get<AuthUser>();
  function logout() {
    tokenStorage.clear();
    navigate("/login", { replace: true });
  }
  return (
    <div className="min-h-screen bg-[#f7f8fd] lg:grid lg:grid-cols-[256px_1fr]">
      <aside className="sticky top-0 hidden h-screen border-r border-slate-300 bg-white px-4 py-7 lg:flex lg:flex-col">
        <Logo className="px-3" to="/expert" />
        <nav className="mt-10 space-y-1">
          {expertNavigation.map(({ icon: Icon, ...item }) => <NavLink key={item.to} to={item.to} end={item.to === "/expert"} className={navClass}><Icon className="size-5" />{item.label}</NavLink>)}
        </nav>
        <div className="mt-auto space-y-1">
          {expertUtilityNavigation.map(({ icon: Icon, ...item }) => <NavLink key={item.to} to={item.to} className={navClass}><Icon className="size-5" />{item.label}</NavLink>)}
          <button className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium text-rose-600 hover:bg-rose-50" onClick={logout}><LogOut className="size-5" />Đăng xuất</button>
          <div className="mt-4 flex items-center gap-3 border-t border-slate-300 px-3 pt-5">
            <Avatar fallback={user?.fullName ?? "CG"} />
            <div className="min-w-0"><p className="truncate text-sm font-bold">{user?.fullName ?? "Chuyên gia"}</p><p className="truncate text-[11px] text-muted">{user?.email}</p></div>
          </div>
        </div>
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-5 border-b border-line bg-white px-5 md:px-8">
          <Logo compact className="lg:hidden" to="/expert" />
          <p className="hidden flex-1 text-sm text-muted md:block">Không gian làm việc chuyên gia</p>
          <Bell className="ml-auto size-5 text-slate-500" />
          <Avatar className="size-8" fallback={user?.fullName ?? "CG"} />
        </header>
        <main className="p-5 md:p-8"><Outlet /></main>
      </div>
    </div>
  );
}

import { Bell, Search } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { Avatar, Logo, cn } from "@/shared";
import { useCurrentUser } from "@/features/auth";
import { expertNavigation, expertUtilityNavigation } from "@/shared/constants/navigation";
import { ExpertAvatarMenu } from "./ExpertAvatarMenu";

const navClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    "flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition",
    isActive ? "bg-emerald-300 text-slate-700" : "text-slate-600 hover:bg-slate-50",
  );

export function ExpertLayout() {
  const user = useCurrentUser();
  const displayName = user.data?.fullName ?? "Chuyên gia";
  const email = user.data?.email ?? "";
  return (
    <div className="min-h-screen bg-[#f7f8fd] lg:grid lg:grid-cols-[256px_1fr]">
      <aside className="sticky top-0 hidden h-screen border-r border-slate-300 bg-white px-4 py-7 lg:flex lg:flex-col">
        <Logo className="px-3" to="/expert" />
        <nav className="mt-10 space-y-1">
          {expertNavigation.map(({ icon: Icon, ...item }) => (
            <NavLink key={item.to} to={item.to} end={item.to === "/expert"} className={navClass}>
              <Icon className="size-5" />{item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto space-y-1">
          <button className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium text-slate-600 hover:bg-slate-50">
            <Bell className="size-5" />Thông báo
          </button>
          {expertUtilityNavigation.map(({ icon: Icon, ...item }) => (
            <NavLink key={item.to} to={item.to} className={navClass}><Icon className="size-5" />{item.label}</NavLink>
          ))}
          <div className="mt-4 flex items-center gap-3 border-t border-slate-300 px-3 pt-5">
            <Avatar fallback={displayName} />
            <div className="min-w-0">
              <p className="truncate text-sm font-bold">{displayName}</p>
              <p className="truncate text-[11px] text-muted">{email}</p>
            </div>
          </div>
        </div>
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-5 border-b border-line bg-white px-5 md:px-8">
          <Logo compact className="lg:hidden" to="/expert" />
          <label className="relative hidden flex-1 md:block">
            <Search className="absolute left-3 top-1/2 size-5 -translate-y-1/2 text-slate-600" />
            <input className="h-10 w-full rounded-full bg-[#f2f5fd] pl-11 pr-4 text-sm outline-none placeholder:text-slate-500 focus:ring-2 focus:ring-brand-500" placeholder="Tìm kiếm bệnh nhân, hồ sơ..." />
          </label>
          <button className="relative ml-auto rounded-full p-2 text-slate-600 hover:bg-slate-100" aria-label="Thông báo">
            <Bell className="size-5" /><span className="absolute right-2 top-1.5 size-2 rounded-full bg-red-500" />
          </button>
          <span className="h-8 w-px bg-slate-300" />
          <ExpertAvatarMenu displayName={displayName} />
        </header>
        <main className="p-5 md:p-8"><Outlet /></main>
      </div>
    </div>
  );
}

import { useState } from "react";
import { Bell, LogOut, Search } from "lucide-react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import type { AuthUser } from "@/features/auth";
import { Avatar, ConfirmDialog, Logo, cn } from "@/shared";
import { expertNavigation, expertUtilityNavigation } from "@/shared/constants/navigation";
import { tokenStorage, userStorage } from "@/shared/lib/storage";

const navClass = ({ isActive }: { isActive: boolean }) => cn("flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition", isActive ? "bg-emerald-300 text-slate-700" : "text-slate-600 hover:bg-slate-50");

export function ExpertLayout() {
  const navigate = useNavigate();
  const user = userStorage.get<AuthUser>();
  const [confirming, setConfirming] = useState(false);
  const [query, setQuery] = useState("");
  function logout() {
    tokenStorage.clear();
    navigate("/login", { replace: true });
  }
  function search(event: React.FormEvent) {
    event.preventDefault();
    const match = expertNavigation.find((item) => item.label.toLowerCase().includes(query.trim().toLowerCase()));
    if (match) navigate(match.to);
  }
  return <div className="min-h-screen bg-[#f7f8fd] lg:grid lg:grid-cols-[256px_1fr]">
    <aside className="sticky top-0 hidden h-screen border-r border-slate-300 bg-white px-4 py-7 lg:flex lg:flex-col"><Logo className="px-3" to="/expert" /><nav className="mt-10 space-y-1">{expertNavigation.map(({ icon: Icon, ...item }) => <NavLink key={item.to} to={item.to} end={item.to === "/expert"} className={navClass}><Icon className="size-5" />{item.label}</NavLink>)}</nav><div className="mt-auto space-y-1">{expertUtilityNavigation.map(({ icon: Icon, ...item }) => <NavLink key={item.to} to={item.to} className={navClass}><Icon className="size-5" />{item.label}</NavLink>)}<button className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium text-rose-600 hover:bg-rose-50" onClick={() => setConfirming(true)}><LogOut className="size-5" />Đăng xuất</button><div className="mt-4 flex items-center gap-3 border-t border-slate-300 px-3 pt-5"><Avatar fallback={user?.fullName ?? "CG"} /><div className="min-w-0"><p className="truncate text-sm font-bold">{user?.fullName ?? "Chuyên gia"}</p><p className="truncate text-[11px] text-muted">{user?.email}</p></div></div></div></aside>
    <div className="min-w-0"><header className="sticky top-0 z-30 flex h-16 items-center gap-5 border-b border-line bg-white px-5 md:px-8"><Logo compact className="lg:hidden" to="/expert" /><form className="relative hidden max-w-md flex-1 md:block" onSubmit={search}><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" /><input className="h-10 w-full rounded-full bg-slate-100 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-brand-500" placeholder="Tìm chức năng chuyên gia..." value={query} onChange={(event) => setQuery(event.target.value)} /></form><Link className="ml-auto text-slate-500" to="/expert/notifications"><Bell className="size-5" /></Link><Avatar className="size-8" fallback={user?.fullName ?? "CG"} /></header><main className="p-5 md:p-8"><Outlet /></main></div>
    <ConfirmDialog open={confirming} title="Đăng xuất khỏi MindCare?" description="Bạn sẽ cần đăng nhập lại để quản lý lịch và hồ sơ chuyên gia." onCancel={() => setConfirming(false)} onConfirm={logout} />
  </div>;
}
